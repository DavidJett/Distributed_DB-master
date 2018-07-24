package transaction;

import lockmgr.DeadlockException;
import sun.misc.InvalidJarIndexException;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

import transaction.Car;
import transaction.Flight;
import transaction.Hotel;
import transaction.WorkflowController;


/**
 * Workflow Controller for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the WC.  In the real
 * implementation, the WC should forward calls to either RM or TM,
 * instead of doing the things itself.
 */

public class WorkflowControllerImpl
        extends java.rmi.server.UnicastRemoteObject
        implements WorkflowController {

    protected int flightcounter, flightprice, carscounter, carsprice, roomscounter, roomsprice;
    protected int xidCounter;

    protected ResourceManager rmFlights = null;
    protected ResourceManager rmRooms = null;
    protected ResourceManager rmCars = null;
    protected ResourceManager rmCustomers = null;
    protected TransactionManager tm = null;

    public static void main(String args[]) {
        System.setSecurityManager(new RMISecurityManager());
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return;
        }
        String rmiPort = prop.getProperty("wc.port");

        try {
            LocateRegistry.createRegistry(Integer.parseInt(rmiPort));
            if (rmiPort == null) {
                rmiPort = "";
            } else if (!rmiPort.equals("")) {
                rmiPort = "//:" + rmiPort + "/";
            }
            WorkflowControllerImpl obj = new WorkflowControllerImpl();
            Naming.rebind(rmiPort + WorkflowController.RMIName, obj);
            System.out.println("WC bound");
        } catch (Exception e) {
            System.err.println("WC not bound:" + e);
            System.exit(1);
        }
    }


    public WorkflowControllerImpl() throws RemoteException {
        flightcounter = 0;
        flightprice = 0;
        carscounter = 0;
        carsprice = 0;
        roomscounter = 0;
        roomsprice = 0;
        flightprice = 0;

        xidCounter = 1;

        while (!reconnect()) {
            // would be better to sleep a while
            try{
                Thread.sleep(800);
            }catch(InterruptedException e){
                //
            }
        }
        new Thread(){
            public void run(){
                while(true){
                    //test whether all the resources are accessible
                    try{
                        if (tm != null)
                            tm.ping();
                        if (rmCars != null)
                            rmCars.ping();
                        if (rmCustomers != null)
                            rmCustomers.ping();
                        if (rmFlights != null)
                            rmFlights.ping();
                        if (rmRooms != null)
                            rmRooms.ping();
                    }catch(Exception e){
                        try{
                            reconnect();
                        }catch (RemoteException e2){
                            e.printStackTrace();
                        }
                    }
                    try{
                        Thread.sleep(800);
                    }catch(InterruptedException e){
                        //
                    }
                }
            }
        }.start();
    }


    // TRANSACTION INTERFACE
    public int start()
            throws RemoteException {
        return (xidCounter++);
    }
    //Commit a transaction
    public int commit(int xid)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        System.out.println("Committing");
        int result=0;
        try{
            if(tm.commit(xid)){
                result=1;
                System.out.print("Finished")
            }
        }catch(TransactionCommitException e){
            result=-1;
        }
        return result;
    }

    public int abort(int xid)
            throws RemoteException,
            InvalidTransactionException {
        System.out.print("Aborting!");
        int result=0;
        try{
            if(tm.abort(xid)){
                result=1;
            }
        }catch(TransactionAbortedException e){
                result=-1;
        }
        System.out.print("Finished!");
        return result;
    }


    // ADMINISTRATIVE INTERFACE
    public boolean addFlight(int xid, String flightNum, int numSeats, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        boolean flag=true;
        try{
            if(xid<0||flightNum==null||numSeats<0)
                return false;
            ResourceItem item=rmFlights.query(xid,WorkflowController.FlightTableName,flightNum);
            int curPrice=0,curSeatNum=0,curAvailNum=0;
            if(item!=null){
                curPrice=price<0?Integer.parseInt(item.getIndex("price").toString()):price;
                curSeatNum=numSeats<0?Integer.parseInt(item.getIndex("seatNum").toString()):Integer.parseInt(item.getIndex("seatNum").toString())+numSeats;
                curAvailNum=numSeats<0?Integer.parseInt(item.getIndex("availNum").toString()):Integer.parseInt(item.getIndex("availNum").toString())+numSeats;
                ResourceItem newItem = new Flight(flightNum, curPrice, curSeatNum, curAvailNum);
                if(rmFlights.update(xid,WorkflowController.FlightTableName,newItem)==false){
                    System.out.println("The addition of Flight:"+flightNum+" failed!");
                    return false;
                }
            }
        }catch(DeadlockException e){
            e.printStackTrace();
        }catch(RemoteException e){
            flag=false;
            try{
                tm.abort(xid);
            }catch(TransactionAbortedException e2){
                e2.printStackTrace();
              }
            }catch(NumberFormatException e){
                e.printStackTrace();
            }catch(InvalidJarIndexException e){
                e.printStackTrace();
            }
            if(flag==false){
                System.out.println("The addition of Flight:"+flightNum+" failed!");
            }
            return flag;
    }
    //problematic
    public boolean deleteFlight(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        int availNum=0,seatNum=0;
        boolean flag=true;
        try{
            ResourceItem item=rmFlights.query(xid,WorkflowController.ReservationTableName,flightNum);
            if(item!=null){
                seatNum=Integer.parseInt(item.getIndex("seatNum").toString());
                availNum=Integer.parseInt(item.getIndex("availNum").toString());
            }
        }catch(RemoteException e){
            flag=false;
            tm.abort(xid);
        }catch(DeadlockException e){
            e.printStackTrace();
        }catch(NumberFormatException e){
            e.printStackTrace();
        }catch(InvalidIndexException e){
            e.printStackTrace();
        }
        if(flag==false) {
            System.out.println("The deletion of Flight has failed!");
        }
        return false;
    }

    public boolean addRooms(int xid, String location, int numRooms, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        boolean flag=true;
        try{
            if(xid<0||location==null||numRooms<0)
                return false;
            ResourceItem item=rmRooms.query(xid,WorkflowController.HotelTableName,location);
            int curPrice=0,curRoomNum=0,curAvailNum=0;
            if(item!=null){
                curPrice=price<0?Integer.parseInt(item.getIndex("price").toString()):price;
                curRoomNum=numRooms<0?Integer.parseInt(item.getIndex("roomNum").toString()):Integer.parseInt(item.getIndex("roomNum").toString())+numRooms;
                curAvailNum=numRooms<0?Integer.parseInt(item.getIndex("availNum").toString()):Integer.parseInt(item.getIndex("availNum").toString())+numRooms;
                ResourceItem newItem = new Hotel(location, curPrice, curRoomNum, curAvailNum);
                if(rmRooms.update(xid,WorkflowController.HotelTableName,newItem)==false){
                    System.out.println("The addition of Hotel:"+location+" failed!");
                    return false;
                }
            }
        }catch(DeadlockException e){
            e.printStackTrace();
        }catch(RemoteException e){
            flag=false;
            try{
                tm.abort(xid);
            }catch(TransactionAbortedException e2){
            }
        }catch(NumberFormatException e){
            e.printStackTrace();
        }catch(InvalidJarIndexException e){
            e.printStackTrace();
        }
        if(flag==false){
            System.out.println("The addition of Hotel:"+location+" failed!");
        }
        return flag;
    }

    public boolean deleteRooms(int xid, String location, int numRooms)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        roomscounter = 0;
        roomsprice = 0;
        return true;
    }

    public boolean addCars(int xid, String location, int numCars, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        carscounter += numCars;
        carsprice = price;
        return true;
    }

    public boolean deleteCars(int xid, String location, int numCars)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        carscounter = 0;
        carsprice = 0;
        return true;
    }

    public boolean newCustomer(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return true;
    }

    public boolean deleteCustomer(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return true;
    }


    // QUERY INTERFACE
    public int queryFlight(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return flightcounter;
    }

    public int queryFlightPrice(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return flightprice;
    }

    public int queryRooms(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return roomscounter;
    }

    public int queryRoomsPrice(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return roomsprice;
    }

    public int queryCars(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return carscounter;
    }

    public int queryCarsPrice(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return carsprice;
    }

    public int queryCustomerBill(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return 0;
    }


    // RESERVATION INTERFACE
    public boolean reserveFlight(int xid, String custName, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        flightcounter--;
        return true;
    }

    public boolean reserveCar(int xid, String custName, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        carscounter--;
        return true;
    }

    public boolean reserveRoom(int xid, String custName, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        roomscounter--;
        return true;
    }

    // TECHNICAL/TESTING INTERFACE
    public boolean reconnect()
            throws RemoteException {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return false;
        }
        String rmiPort[] = new String[5];
        rmiPort[0] = prop.getProperty("rm.RMFlights.port");
        rmiPort[1] = prop.getProperty("rm.RMRooms.port");
        rmiPort[2] = prop.getProperty("rm.RMCars.port");
        rmiPort[3] = prop.getProperty("rm.RMCustomers.port");
        rmiPort[4] = prop.getProperty("tm.port");
        for (int i = 0; i < 5; i++) {
            if (rmiPort[i] == null) {
                rmiPort[i] = "";
            } else if (!rmiPort[i].equals("")) {
                rmiPort[i] = "//:" + rmiPort[i] + "/";
            }

        }

        try {
            rmFlights =
                    (ResourceManager) Naming.lookup(rmiPort[0] +
                            ResourceManager.RMINameFlights);
            System.out.println("WC bound to RMFlights");
            rmRooms =
                    (ResourceManager) Naming.lookup(rmiPort[1] +
                            ResourceManager.RMINameRooms);
            System.out.println("WC bound to RMRooms");
            rmCars =
                    (ResourceManager) Naming.lookup(rmiPort[2] +
                            ResourceManager.RMINameCars);
            System.out.println("WC bound to RMCars");
            rmCustomers =
                    (ResourceManager) Naming.lookup(rmiPort[3] +
                            ResourceManager.RMINameCustomers);
            System.out.println("WC bound to RMCustomers");
            tm =
                    (TransactionManager) Naming.lookup(rmiPort[4] +
                            TransactionManager.RMIName);
            System.out.println("WC bound to TM");
        } catch (Exception e) {
            System.err.println("WC cannot bind to some component:" + e);
            return false;
        }

        try {
            if (rmFlights.reconnect() && rmRooms.reconnect() &&
                    rmCars.reconnect() && rmCustomers.reconnect()) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("Some RM cannot reconnect:" + e);
            return false;
        }

        return false;
    }

    public boolean dieNow(String who)
            throws RemoteException {
        if (who.equals(TransactionManager.RMIName) ||
                who.equals("ALL")) {
            try {
                tm.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameFlights) ||
                who.equals("ALL")) {
            try {
                rmFlights.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameRooms) ||
                who.equals("ALL")) {
            try {
                rmRooms.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameCars) ||
                who.equals("ALL")) {
            try {
                rmCars.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameCustomers) ||
                who.equals("ALL")) {
            try {
                rmCustomers.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(WorkflowController.RMIName) ||
                who.equals("ALL")) {
            System.exit(1);
        }
        return true;
    }

    public boolean dieRMAfterEnlist(String who)
            throws RemoteException {
        return true;
    }

    public boolean dieRMBeforePrepare(String who)
            throws RemoteException {
        return true;
    }

    public boolean dieRMAfterPrepare(String who)
            throws RemoteException {
        return true;
    }

    public boolean dieTMBeforeCommit()
            throws RemoteException {
        return true;
    }

    public boolean dieTMAfterCommit()
            throws RemoteException {
        return true;
    }

    public boolean dieRMBeforeCommit(String who)
            throws RemoteException {
        return true;
    }

    public boolean dieRMBeforeAbort(String who)
            throws RemoteException {
        return true;
    }
}
