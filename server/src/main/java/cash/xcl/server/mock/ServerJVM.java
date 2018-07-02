package cash.xcl.server.mock;

import cash.xcl.api.tcp.XCLServer;
import cash.xcl.server.Gateway;
import cash.xcl.server.VanillaGateway;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.salt.Ed25519;

public class ServerJVM implements Closeable, Runnable {
    public static boolean INTERNAL = Boolean.getBoolean("internal");
    public static int DEFAULT_SERVER_ADDRESS = 10001;
    private XCLServer server;
    private Gateway gateway;

    public static void main(String[] args) {
        Bytes publicKey = Bytes.allocateDirect(Ed25519.PUBLIC_KEY_LENGTH);
        Bytes secretKey = Bytes.allocateDirect(Ed25519.SECRET_KEY_LENGTH);
        Ed25519.generatePublicAndSecretKey(publicKey, secretKey);
        try {
            new ServerJVM(DEFAULT_SERVER_ADDRESS, secretKey,2500, 1000, 1, publicKey);
            while(true) {
                System.out.println("ServerJVM is running... ");
                Thread.sleep(10000);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            //Jvm.pause(1000);
            //benchmarkMain.close();
            System.out.println("exiting");
            System.exit(0);
        }
    }

    public ServerJVM(int serverAddress,
                     Bytes secretKey,
                     int mainBlockPeriodMS,
                     int localBlockPeriodMS,
                     long sourceAddress,
                     Bytes publicKey) {
        try {
            long[] clusterAddresses = {serverAddress};
            this.gateway = VanillaGateway.newGateway(serverAddress, "gb1dn", clusterAddresses, mainBlockPeriodMS, localBlockPeriodMS);
            this.server = new XCLServer("one", serverAddress, serverAddress, publicKey, secretKey, gateway);
            //TODO : This has to be reinstated later.
            //this.server.internal(INTERNAL);
            this.server.internal(true);
            this.gateway.start();

            register(sourceAddress, publicKey);

        } catch (Throwable t) {
            t.printStackTrace();
            close();
        }
    }


    public ServerJVM() {

        Bytes publicKey = Bytes.allocateDirect(Ed25519.PUBLIC_KEY_LENGTH);
        Bytes secretKey = Bytes.allocateDirect(Ed25519.SECRET_KEY_LENGTH);
        Ed25519.generatePublicAndSecretKey(publicKey, secretKey);
        try {
            new ServerJVM(DEFAULT_SERVER_ADDRESS, secretKey,2500, 1000, 1, publicKey);
            System.out.println("ServerJVM is running... ");
        } catch (Throwable t) {
            t.printStackTrace();

        } finally {
            //Jvm.pause(1000);
            //benchmarkMain.close();
            System.out.println("exiting");
        }
    }

    @Override
    public void close() {
        // todo: quietly terminate the BlockEngine voting thread

        //this.gateway.close();
        Closeable.closeQuietly(this.server);
    }

    public void register(long address, Bytes<?> publicKey) {

        this.server.register(address, publicKey);
    }

    @Override
    public void run() {
        Bytes publicKey = Bytes.allocateDirect(Ed25519.PUBLIC_KEY_LENGTH);
        Bytes secretKey = Bytes.allocateDirect(Ed25519.SECRET_KEY_LENGTH);
        Ed25519.generatePublicAndSecretKey(publicKey, secretKey);
        try {
            new ServerJVM(DEFAULT_SERVER_ADDRESS, secretKey,2500, 1000, 1, publicKey);
            while(true) {
                System.out.println("ServerJVM is running... ");
                Thread.sleep(10000);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            //Jvm.pause(1000);
            //benchmarkMain.close();
            System.out.println("exiting");
            System.exit(0);
        }
    }
}
