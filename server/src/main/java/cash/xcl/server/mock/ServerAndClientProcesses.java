package cash.xcl.server.mock;

import cash.xcl.api.AllMessages;
import cash.xcl.api.dto.*;
import cash.xcl.api.tcp.WritingAllMessages;
import cash.xcl.api.tcp.XCLClient;
import cash.xcl.api.tcp.XCLServer;
import cash.xcl.api.util.AbstractAllMessages;
import cash.xcl.server.Gateway;
import cash.xcl.server.VanillaGateway;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.salt.Ed25519;

import java.util.concurrent.atomic.AtomicInteger;



public class ServerAndClientProcesses extends AbstractAllMessages {

    private XCLServer server;
    private Gateway gateway;
    private int serverAddress = 10001;
    private int sourceAddress = 1;
    private int destinationAddress = 2;


    private XCLClient client;

    private Bytes publicKey = Bytes.allocateDirect(Ed25519.PUBLIC_KEY_LENGTH);
    private Bytes secretKey = Bytes.allocateDirect(Ed25519.SECRET_KEY_LENGTH);


    public ServerAndClientProcesses(int mainBlockPeriodMS,
                                    int localBlockPeriodMS,
                                    @NotNull final AllMessages allMessageListener) {
        super(99999999999999L);

        try {
            Ed25519.generatePublicAndSecretKey(publicKey, secretKey);

            long[] clusterAddresses = {serverAddress};
            this.gateway = VanillaGateway.newGateway(serverAddress, "gb1dn", clusterAddresses, mainBlockPeriodMS, localBlockPeriodMS);
            this.server = new XCLServer("one", serverAddress, serverAddress, publicKey, secretKey, gateway);
            gateway.start();

            client = new XCLClient("client", "localhost", serverAddress, sourceAddress, secretKey,
                            allMessageListener);

            final OpeningBalanceEvent obe1 = new OpeningBalanceEvent(sourceAddress,
                    1,
                    sourceAddress,
                    "USD",
                    1000);
            client.openingBalanceEvent(obe1);

            final OpeningBalanceEvent obe2 = new OpeningBalanceEvent(sourceAddress,
                    1,
                    destinationAddress,
                    "USD",
                    1000);
            client.openingBalanceEvent(obe2);
        } catch (Throwable t) {
            t.printStackTrace();

        } finally {
            //Jvm.pause(1000);
            //benchmarkMain.close();
        }

    }

    public void initializeAccounts(long sourceAddress,
                                   long destinationAddress) {
        try {
            // source address
            gateway.createNewAddressEvent(new CreateNewAddressEvent(sourceAddress,
                    0, 0, 0,
                    sourceAddress, publicKey));
            final OpeningBalanceEvent obe2 = new OpeningBalanceEvent(
                    sourceAddress,
                    1,
                    sourceAddress,
                    "USD",
                    1000);
            client.openingBalanceEvent(obe2);


            // destinationAddress
            gateway.createNewAddressEvent(new CreateNewAddressEvent(destinationAddress,
                    0, 0, 0,
                    destinationAddress, publicKey));
            final OpeningBalanceEvent obe1 = new OpeningBalanceEvent(
                    sourceAddress,
                    1,
                    destinationAddress,
                    "USD",
                    1000);
            client.openingBalanceEvent(obe1);

        } catch (Throwable t) {
            t.printStackTrace();

        } finally {
            //Jvm.pause(1000);
            //benchmarkMain.close();
        }
    }


    public void transfer(TransferValueCommand tvc1) {
        initializeAccounts( tvc1.sourceAddress(), tvc1.toAddress());
        client.transferValueCommand(tvc1);
    }

    public void exchangeRateQuery(ExchangeRateQuery exchangeRateQuery) {
        gateway.exchangeRateQuery(exchangeRateQuery);
    }

    public void currentBalanceQuery(CurrentBalanceQuery currentBalanceQuery) {
        gateway.currentBalanceQuery(currentBalanceQuery);
    }


    public void close() {
        Closeable.closeQuietly(server);
    }




    @Override
    public void createNewAddressEvent(CreateNewAddressEvent createNewAddressEvent) {
        this.client.createNewAddressEvent(createNewAddressEvent);
    }
    @Override
    public void openingBalanceEvent(OpeningBalanceEvent openingBalanceEvent) {
        this.client.openingBalanceEvent(openingBalanceEvent);
    }
    @Override
    public void transferValueCommand(TransferValueCommand transferValueCommand) {
        this.client.transferValueCommand(transferValueCommand);
    }



    private static class MyWritingAllMessages extends WritingAllMessages {
        private final AtomicInteger count;

        public MyWritingAllMessages(AtomicInteger count) {
            this.count = count;
        }

        @Override
        public WritingAllMessages to(long addressOrRegion) {
            return this;
        }

        @Override
        public void write(SignedBinaryMessage message) {
            System.out.println("message received " + message);
            count.incrementAndGet();
        }

        @Override
        public void transferValueEvent(TransferValueEvent transferValueEvent) {
            System.out.println("message received " + transferValueEvent);
        }


        @Override
        public void close() {

        }
    }




    // Not using JUnit at the moment because
    // on Windows, using JUnit and the native encryption library will crash the JVM.
    public static void main(String[] args) {
        ServerAndClientProcesses benchmarkMain = null;
        try {

            AtomicInteger count = new AtomicInteger();
            MyWritingAllMessages myWritingAllMessages = new MyWritingAllMessages(count);
            benchmarkMain = new ServerAndClientProcesses(1000, 10, myWritingAllMessages);

            System.out.println("before transfer");
            TransferValueCommand tvc1 = new TransferValueCommand(1, 0, 2, 1e-9, "USD", "");
            benchmarkMain.transfer(tvc1);
            System.out.println("after transfer");

            TransactionBlockEvent.printNumberOfObjects();
            System.out.println("before print");

            ((VanillaGateway) benchmarkMain.gateway).printBalances();
            System.out.println("after print");

        } catch (Throwable t) {
            t.printStackTrace();

        } finally {
            //Jvm.pause(1000);
            //benchmarkMain.close();
            System.exit(0);
        }

    }


}
