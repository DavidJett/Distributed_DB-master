package transaction;

import java.io.FileInputStream;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.util.*;

/**
 * Transaction Manager for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the TM
 */

public class TransactionManagerImpl
        extends java.rmi.server.UnicastRemoteObject
        implements TransactionManager {
    protected int totalXid;
    protected Hashtable<Integer, HashMap<String, ResourceManager>> rmJoin;
    protected Hashtable<Integer, String> toDoList;
    protected String dieTime = "NoDie";
    public static void main(String args[]) {
        System.setSecurityManager(new RMISecurityManager());
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return;
        }
        String rmiPort = prop.getProperty("tm.port");
        try {
            LocateRegistry.createRegistry(Integer.parseInt(rmiPort));
            if (rmiPort == null) {
                rmiPort = "";
            } else if (!rmiPort.equals("")) {
                rmiPort = "//:" + rmiPort + "/";
            }
            TransactionManagerImpl obj = new TransactionManagerImpl();
            Naming.rebind(rmiPort + TransactionManager.RMIName, obj);
            System.out.println("TM bound");
        } catch (Exception e) {
            System.err.println("TM not bound:" + e);
            System.exit(1);
        }
    }

    public void ping() throws RemoteException {
    }

    public void enlist(int xid, ResourceManager rm) throws RemoteException {
        if(toDoList.containsKey(xid)){
            HashMap<String, ResourceManager> rmList = rmJoin.get(xid);
            String operation = toDoList.get(xid);
            if(operation.equals("commit")){
                try{
                    rm.commit(xid);
                    rmList.remove(rm.getID());
                    System.out.println("An RM commited in transaction "+xid);
                } catch(Exception e){
                    System.err.println("An RM failed to commit in transaction"+xid);
                }
                if(rmList.isEmpty()){
                    toDoList.remove(xid);
                    rmJoin.remove(xid);
                    System.out.println("Transaction "+xid+" commited");
                }
            }
            else{
                try{
                    rm.abort(xid);
                    rmList.remove(rm.getID());
                    System.out.println("An RM aborted in transaction "+xid);
                } catch(Exception e){
                    System.err.println("An RM failed to abort in transaction"+xid);
                }
                if(rmList.isEmpty()){
                    toDoList.remove(xid);
                    rmJoin.remove(xid);
                    System.out.println("Transaction "+xid+" aborted");
                }
            }
        }
        else if (rmJoin.containsKey(xid)){
            HashMap<String, ResourceManager> rmList = (HashMap<String, ResourceManager>)rmJoin.get(xid);
            rmList.put(rm.getID(),rm);
        }
        else{
            HashMap<String, ResourceManager> rmList = new HashMap<String, ResourceManager>();
            rmList.put(rm.getID(),rm);
            rmJoin.put(xid, rmList);
        }
    }

    public TransactionManagerImpl() throws RemoteException {
    }

    @Override
    public int start() {
        return totalXid++;
    }

    @Override
    public boolean commit(int xid) throws RemoteException, InvalidTransactionException, TransactionCommittedException, TransactionAbortedException {
        if(dieTime.equals("BeforeCommit")){
            dieNow();
        }
        if(xid < 0){
            throw new InvalidTransactionException(xid, "Negative xid found");
        }
        if(!rmJoin.containsKey(xid)){
            throw new InvalidTransactionException(xid,"An unexpected transaction "+xid+" tried to commit");
        }
        HashMap<String, ResourceManager> rmList = rmJoin.get(xid);
        Iterator iter = rmList.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry en = (Map.Entry)iter.next();
            ResourceManager rm = (ResourceManager)en.getValue();
            try{
                if(!rm.prepare(xid)){
                    System.out.println("An RM is not prepared. Refuse to commit");
                    return false;
                }
            } catch(RemoteException e){
                abort(xid);
                throw new TransactionCommittedException(xid,"Some RMs failed when preparing");
            }
        }
        toDoList.put(xid, "commit");
        boolean isSuccess = true;
        Iterator iterR = rmList.entrySet().iterator();
        while(iterR.hasNext()){
            Map.Entry en = (Map.Entry)iterR.next();
            ResourceManager rm = (ResourceManager)en.getValue();
            try{
                rm.commit(xid);
                iterR.remove();
            } catch(RemoteException e){
                isSuccess = false;
            }
        }
        if(!isSuccess){
            System.err.println("Transaction "+xid+" failed to commit due to RM commit failure");
            throw new TransactionCommittedException(xid,"Transaction failed to commit due to RM commit failure");
        }
        if(rmList.isEmpty()){
            toDoList.remove(xid);
            rmJoin.remove(xid);
            System.out.println("Transaction "+xid+" committed");
            if(dieTime.equals("AfterCommit")){    //dieTM : after commit
                dieNow();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean abort(int xid) throws RemoteException,InvalidTransactionException, TransactionAbortedException{
        if(xid < 0){
            throw new InvalidTransactionException(xid, "Negative xid found");
        }
        if(!rmJoin.containsKey(xid)){
            throw new InvalidTransactionException(xid,"An unexpected transaction "+xid+" tried to abort");
        }
        toDoList.put(xid,"abort");
        boolean isSuccess = true;
        HashMap<String, ResourceManager> rmList = rmJoin.get(xid);
        Iterator iter = rmList.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry en = (Map.Entry)iter.next();
            ResourceManager rm = (ResourceManager)en.getValue();
            try{
                rm.abort(xid);
                iter.remove();
            }catch(RemoteException e){
                isSuccess = false;
            }
        }
        if(!isSuccess){
            System.err.println("Transaction "+xid+" failed to abort due to RM abort failure");
            throw new TransactionAbortedException(xid,"The transaction fails to abort!");
        }
        if(rmList.isEmpty()){
            toDoList.remove(xid);
            rmJoin.remove(xid);
            System.out.println("Transaction "+xid+"is aborted");
            return true;
        }
        return false;
    }

    public boolean dieNow()
            throws RemoteException {
        System.exit(1);
        return true; // We won't ever get here since we exited above;
        // but we still need it to please the compiler.
    }
    public void setDieTime(String mode) throws RemoteException
    {
        dieTime = mode;
        System.out.println("Die time has been set to : " + mode);
    }
}
