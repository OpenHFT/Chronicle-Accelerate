package cash.xcl.util;

import net.openhft.chronicle.bytes.Bytes;

public interface PublicKeyRegistry {
    void register(long address, Bytes<?> publicKey);

    Boolean verify(long address, Bytes<?> sigAndMsg);

    boolean internal();

    PublicKeyRegistry internal(boolean internal);
}
