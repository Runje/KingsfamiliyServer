package database;

import com.koenig.commonModel.Item;

import java.nio.ByteBuffer;


public class TransactionID extends Item {
    public TransactionID(String id) {
        super(id, "");
    }

    public TransactionID(ByteBuffer buffer) {
        super(buffer);
    }
}
