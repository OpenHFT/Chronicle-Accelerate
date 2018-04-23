package cash.xcl.api.exch;

import cash.xcl.api.dto.MessageTypes;
import cash.xcl.api.dto.SignedBinaryMessage;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;


public class ExecutionReportEvent extends SignedBinaryMessage {

    private ExecutionReport executionReport;

    public ExecutionReportEvent(long sourceAddress, long eventTime, ExecutionReport executionReport) {
        super(sourceAddress, eventTime);
        this.executionReport = executionReport;
    }

    public ExecutionReportEvent() {
    }

    @Override
    protected void readMarshallable2(BytesIn<?> bytes) {
        if (executionReport == null) {
            executionReport = new ExecutionReport();
        }

        executionReport.readMarshallable(bytes);
    }

    @Override
    protected void writeMarshallable2(BytesOut<?> bytes) {
        executionReport.writeMarshallable(bytes);
    }

    @Override
    public int intMessageType() {
        return MessageTypes.EXECUTION_REPORT;
    }

}
