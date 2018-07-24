package transaction;

import java.io.Serializable;

/**
 * @author jett
 */
public class Flight implements ResourceItem, Serializable {

    public static final String INDEX_LOCATION = "location";

    protected String location;
    protected int price;
    protected int seatNum;
    protected int availNum;

    protected boolean isdeleted = false;

    public Flight(String location, int price, int seatNum, int availNum) {
        this.location = location;
        this.price = price;
        this.seatNum = seatNum;
        this.availNum = availNum;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getSeatNum() {
        return seatNum;
    }

    public void setSeatNum(int roomNum) {
        this.seatNum = seatNum;
    }

    public int getAvailNum() {
        return availNum;
    }

    public void setAvailNum(int availNum) {
        this.availNum = availNum;
    }

    public String[] getColumnNames() {
        // TODO Auto-generated method stub
        return new String[]{"location", "price", "seatNum", "availNum"};
    }

    public String[] getColumnValues() {
        // TODO Auto-generated method stub
        return new String[]{location, "" + price, "" + seatNum, "" + availNum};
    }

    public Object getIndex(String indexName) throws InvalidIndexException {
        // TODO Auto-generated method stub
        if (indexName.equals(INDEX_LOCATION)) {
            return location;
        } else if (indexName.equals("price")) {
            return price;
        } else if (indexName.equals("seatNum")) {
            return seatNum;
        } else if (indexName.equals("availNum")) {
            return availNum;
        } else {
            throw new InvalidIndexException(indexName);
        }
    }

    public Object getKey() {
        return location;
    }

    public boolean isDeleted() {

        return isdeleted;
    }

    public void delete() {

        isdeleted = true;
    }

    public Object clone() {
        Flight f = new Flight(getLocation(), getPrice(), getSeatNum(), getAvailNum());
        f.isdeleted = isdeleted;
        return f;
    }
}
