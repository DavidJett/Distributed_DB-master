package transaction;

import java.io.Serializable;

/**
 * @author jett
 */
public class Car implements ResourceItem, Serializable {

    public static final String INDEX_LOCATION = "location";

    protected String location;
    protected int price;
    protected int carNum;
    protected int availNum;

    protected boolean isdeleted = false;

    public Car(String location, int price, int carNum, int availNum) {
        this.location = location;
        this.price = price;
        this.carNum = carNum;
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

    public int getCarNum() {
        return carNum;
    }

    public void setCarNum(int carNum) {
        this.carNum = carNum;
    }

    public int getAvailNum() {
        return availNum;
    }

    public void setAvailNum(int numAvail) {
        this.availNum = availNum;
    }

    public String[] getColumnNames() {
        // TODO Auto-generated method stub
        return new String[]{"location", "price", "carNum", "availNum"};
    }

    public String[] getColumnValues() {
        // TODO Auto-generated method stub
        return new String[]{location, "" + price, "" + carNum, "" + availNum};
    }

    public Object getIndex(String indexName) throws InvalidIndexException {
        // TODO Auto-generated method stub
        if (indexName.equals(INDEX_LOCATION)) {
            return location;
        } else if (indexName.equals("price")) {
            return price;
        } else if (indexName.equals("carNum")) {
            return carNum;
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
        Car c = new Car(getLocation(), getPrice(), getCarNum(), getAvailNum());
        c.isdeleted = isdeleted;
        return c;
    }
}
