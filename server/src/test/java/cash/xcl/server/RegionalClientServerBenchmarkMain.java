package cash.xcl.server;

import cash.xcl.api.AllMessages;
import cash.xcl.api.dto.*;
import cash.xcl.api.tcp.XCLClient;
import cash.xcl.api.tcp.XCLServer;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/*
-XX:+UnlockCommercialFeatures
-XX:+FlightRecorder
-XX:+UnlockDiagnosticVMOptions
-XX:+DebugNonSafepoints
-XX:StartFlightRecording=name=test,filename=test.jfr,dumponexit=true,settings=profile
-DfastJava8IO=true

Intel(R) Core(TM) i7-7820X CPU @ 3.60GHz, Centos 7 with linux 4.12
TBA

Intel i7-7700 CPU, Windows 10, 32 GB memory.
benchmark - oneThread = 331318 sustained, 416312 burst messages per second
benchmark - twoThreads = 464425 sustained, 755085 burst messages per second
benchmark - fourThreads = 465087 sustained, 994536 burst messages per second
benchmark - eightThreads = 431039 sustained, 808222 burst messages per second

Intel i7-7820X, Windows 10, 64 GB memory.
as a core service
benchmark - oneThread = 372916 sustained, 476293 burst messages per second
benchmark - twoThreads = 441794 sustained, 686253 burst messages per second
benchmark - fourThreads = 421588 sustained, 743256 burst messages per second
benchmark - eightThreads = 393173 sustained, 683544 burst messages per second

with verify/sign
benchmark - oneThread = 9652 sustained, 180129 burst messages per second
benchmark - twoThreads = 18885 sustained, 441968 burst messages per second
benchmark - fourThreads = 32032 sustained, 576003 burst messages per second
benchmark - eightThreads = 52218 sustained, 489249 burst messages per second
*/

public class RegionalClientServerBenchmarkMain {

    static final boolean INTERNAL = Boolean.getBoolean("internal");
    final Set<Integer> openingSet = new HashSet<>();
    private final XCLBuilder builder;
    private final XCLServer server;
    private int serverAddress = 10001;
    private final AllMessages gateway;

    public RegionalClientServerBenchmarkMain(int mainBlockPeriodMS,
                                             int localBlockPeriodMS,
                                             int iterations,
                                             int clientThreads) throws IOException {

        builder = new XCLBuilder()
                .mainBlockPeriodMS(mainBlockPeriodMS)
                .localBlockPeriodMS(localBlockPeriodMS)
                .internal(INTERNAL)
                .serverAddress(serverAddress);
        this.server = builder.createServer(serverAddress);
        gateway = server.gatewayFor(AllMessages.class);

        // register all the addresses involved in the transfers
        // -source and destination accounts- in the Account Service with a opening balance of $1,000,000,000
        for (int iterationNumber = 0; iterationNumber < iterations; iterationNumber++) {
            for (int s = 0; s < clientThreads; s++) {
                final int sourceAddress = (iterationNumber * 100) + s + 1;
                final int destinationAddress = sourceAddress + 1000000;
                gateway.createNewAddressEvent(new CreateNewAddressEvent(0, 0, 0, 0,
                        sourceAddress, server.publicKey()));

                gateway.createNewAddressEvent(new CreateNewAddressEvent(0, 0, 0, 0,
                        destinationAddress, server.publicKey()));

// TODO
                ExchangeRateQuery err = new ExchangeRateQuery(0, 0, "XCL", "USD");
                gateway.exchangeRateQuery(err);

                CurrentBalanceQuery cbq = new CurrentBalanceQuery(0, 0, 1000);
                gateway.currentBalanceQuery(cbq);
            }
        }
    }

    void sendOpeningBalance(XCLClient client, int sourceAddress, int destinationAddress) {
        synchronized (openingSet) {
            if (!openingSet.add(destinationAddress))
                return;
        }
        final OpeningBalanceEvent obe1 = new OpeningBalanceEvent(sourceAddress,
                1,
                destinationAddress,
                "USD",
                1000);
        client.openingBalanceEvent(obe1);
    }

    // Not using JUnit at the moment because
    // on Windows, using JUnit and the native encryption library will crash the JVM.
    public static void main(String[] args) {
        RegionalClientServerBenchmarkMain benchmarkMain = null;
        try {
            int iterations = 3;
            int transfers = INTERNAL ? 1_000_000 : 50_000;
            int total = iterations * transfers;
            benchmarkMain = new RegionalClientServerBenchmarkMain(1000, 10, 10, 8);

            String oneThread = null;
            String twoThreads = null;
            String fourThreads = null;
            String eightThreads = null;
            if (INTERNAL)
                oneThread = benchmarkMain.benchmark(iterations, 1, transfers);
            if (false)
                twoThreads = benchmarkMain.benchmark(iterations, 2, transfers);
            if (false)
                fourThreads = benchmarkMain.benchmark(iterations, 4, transfers);
            if (!INTERNAL)
                eightThreads = benchmarkMain.benchmark(iterations, 8, transfers);
            System.out.println("Total number of messages per benchmark = " + total);
            System.out.println("Including signing and verifying = " + !INTERNAL);
            if (oneThread != null)
                System.out.println("benchmark - oneThread = " + oneThread + " messages per second");
            if (twoThreads != null)
                System.out.println("benchmark - twoThreads = " + twoThreads + " messages per second");
            if (fourThreads != null)
                System.out.println("benchmark - fourThreads = " + fourThreads + " messages per second");
            if (eightThreads != null)
                System.out.println("benchmark - eightThreads = " + eightThreads + " messages per second");

            TransactionBlockEvent.printNumberOfObjects();

        } catch (Throwable t) {
            t.printStackTrace();

        } finally {
            //Jvm.pause(1000);
            //benchmarkMain.close();
            System.exit(0);
        }
    }

    private String benchmark(int iterations, int clientThreads, int transfers) throws InterruptedException, ExecutionException {

        Thread.sleep(2000);

        System.out.println(" STARTING BENCHMARK TEST **********************************************");
        // number of messages = msgs * number of iterations
        // number of messages = 10000 * 10 = 100,000
        double allIterationsTotalTimeSecs = 0;
        double totalSentTime = 0;
        for (int iterationNumber = 0; iterationNumber < iterations; iterationNumber++) {
            long start = System.nanoTime();
            ExecutorService service = Executors.newFixedThreadPool(clientThreads);
            BlockingQueue<String> sent = new LinkedBlockingQueue<>(4);
            List<Future> futures = new ArrayList<>();
            long sentStart = System.nanoTime();
            for (int s = 0; s < clientThreads; s++) {
                final int sourceAddress = (iterationNumber * 100) + s + 1;
                final int destinationAddress = sourceAddress + 1000000;

                futures.add(service.submit((Callable<Void>) () -> {
                    try {
                        AtomicInteger count = new AtomicInteger();
                        AllMessages queuing = new MyWritingAllMessages(count);
                        XCLClient client = builder.createClient("client", "localhost", server.port(), sourceAddress, queuing);
                        sendOpeningBalance(client, sourceAddress, sourceAddress);
                        sendOpeningBalance(client, sourceAddress, destinationAddress);
                        client.subscriptionQuery(new SubscriptionQuery(sourceAddress, 0));
                        TransferValueCommand tvc1 = new TransferValueCommand(sourceAddress, 0, destinationAddress, 1e-9, "USD", "");
                        int c = 0;
                        for (int i = 0; i < transfers; i += clientThreads) {
                            client.transferValueCommand(tvc1);
                            if (++c > 10000 && c % 1000 == 0)
                                Jvm.pause(1);
                        }
                        sent.add("DONE");
                        long last = System.currentTimeMillis() + 1000;
                        for (int i = 0; i < transfers; i += clientThreads) {
                            while (count.get() <= 0) {
                                if (System.currentTimeMillis() > last + 1000) {
                                    System.out.println("pause " + i);
                                    last = System.currentTimeMillis();
                                }
                                Jvm.pause(10);
                            }
                            count.decrementAndGet();
                        }
                        //client.close();
                        Closeable.closeQuietly(client);
                        // +2 for the opening balances.
                        assertEquals(1, count.get(), 1);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return null;
                }));
            }
            for (Future future : futures)
                sent.take();
            double sentTime = (System.nanoTime() - sentStart) / 1e9;
            totalSentTime += sentTime;
            for (Future future : futures)
                future.get();

            double time = (System.nanoTime() - start) / 1e9;

            System.out.printf("Iteration %d - Throughput: %,d sustained, %,d burst messages per sec%n",
                    iterationNumber, (int) (transfers / time), (int) (transfers / sentTime));
            allIterationsTotalTimeSecs += time;
            service.shutdown();
        }

        int sentAverage = (int) (transfers * iterations / totalSentTime);
        int average = (int) (transfers * iterations / allIterationsTotalTimeSecs);

        System.out.printf("Average Throughput after sending %d messages (%d messages * %d times) using %d client threads = %,d / sec, %,d /sec burst%n",
                transfers * iterations,
                transfers,
                iterations,
                clientThreads,
                average,
                sentAverage);

        //((VanillaGateway) gateway).printBalances();

        return average + " sustained, " + sentAverage + " burst";
    }

    public void close() {
        Closeable.closeQuietly(server);
    }
}
