package transaction;

import java.io.Serializable;

/**
 * @author jett
 */
public class Hotel implements ResourceItem, Serializable {

    public static final String INDEX_LOCATION = "location";

    protected String location;
    protected int price;
    protected int roomNum;
    protected int availNum;

    protected boolean isdeleted = false;

    public Hotel(String location, int price, int roomNum, int availNum) {
        this.location = location;
        this.price = price;
        this.roomNum = roomNum;
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

    public int getRoomNum() {
        return roomNum;
    }

    public void setRoomNum(int roomNum) {
        this.roomNum = roomNum;
    }

    public int getAvailNum() {
        return availNum;
    }

    public void setAvailNum(int availNum) {
        this.availNum = availNum;
    }

    public String[] getColumnNames() {
        // TODO Auto-generated method stub
        return new String[]{"location", "price", "roomNum", "availNum"};
    }

    public String[] getColumnValues() {
        // TODO Auto-generated method stub
        return new String[]{location, "" + price, "" + roomNum, "" + availNum};
    }

    public Object getIndex(String indexName) throws InvalidIndexException {
        // TODO Auto-generated method stub
        if (indexName.equals(INDEX_LOCATION)) {
            return location;
        } else if (indexName.equals("price")) {
            return price;
        } else if (indexName.equals("roomNum")) {
            return roomNum;
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
        Hotel h = new Hotel(getLocation(), getPrice(), getRoomNum(), getAvailNum());
        h.isdeleted = isdeleted;
        return h;
    }
}
