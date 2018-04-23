package cash.xcl.api.exch;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.AbstractBytesMarshallable;

import static cash.xcl.util.Validators.*;

public class ExecutionReport extends AbstractBytesMarshallable {

    private CurrencyPair pair;
    private Side action;
    private double quantity;
    private double price;
    private long initiator;
    private long aggressor;

    ExecutionReport() {
    
    }

    public ExecutionReport(CurrencyPair pair, Side action, double quantity, double price, long aggressor, long initiator) {
        assert initiator != aggressor;
        setPair(pair);
        setAction(action);
        setQuantity(quantity);
        setPrice(price);
        this.initiator = initiator;
        this.aggressor = aggressor;
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public void readMarshallable(BytesIn bytes) throws IORuntimeException {
        if (pair == null) {
            pair = new CurrencyPair();
        }
        pair.readMarshallable(bytes);
        setAction(Side.fromId(bytes.readInt()));
        setQuantity(bytes.readDouble());
        setPrice(bytes.readDouble());
        this.aggressor = bytes.readLong();
        this.initiator = bytes.readLong();
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public void writeMarshallable(BytesOut bytes) {
        pair.writeMarshallable(bytes);
        bytes.writeInt(action.ordinal());
        bytes.writeDouble(quantity);
        bytes.writeDouble(price);
        bytes.writeLong(aggressor);
        bytes.writeLong(initiator);
    }

    public CurrencyPair getPair() {
        return pair;
    }

    void setPair(CurrencyPair pair) {
        this.pair = notNull(pair);
    }

    public Side getAction() {
        return action;
    }

    void setAction(Side action) {
        this.action = notNull(action);
    }

    public double getQuantity() {
        return quantity;
    }

    void setQuantity(double quantity) {
        this.quantity = positive(quantity);
    }

    public double getPrice() {
        return price;
    }

    void setPrice(double price) {
        this.price = validNumber(price);
    }

    public long getInitiator() {
        return initiator;
    }


    public long getAggressor() {
        return aggressor;
    }



}
