package cash.xcl.api.dto;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;

// FIXME needs reviewing/completing
// returns the status of the nodes in the current cluster
public class ClusterStatusResponse extends SignedBinaryMessage {

    private ClusterStatusQuery clusterStatusQuery;

    // cluster contains the nodes
    // and each node contains its own status
    private Cluster cluster;

    public ClusterStatusResponse(ClusterStatusQuery clusterStatusQuery, Cluster cluster) {
        this.clusterStatusQuery = clusterStatusQuery;
        this.cluster = cluster;
    }

    public ClusterStatusResponse(long sourceAddress, long eventTime, ClusterStatusQuery clusterStatusQuery, Cluster cluster) {
        super(sourceAddress, eventTime);
        this.clusterStatusQuery = clusterStatusQuery;
        this.cluster = cluster;
    }

    public ClusterStatusResponse() {
        super();
    }

    @Override
    protected void readMarshallable2(BytesIn<?> bytes) {
    }

    @Override
    protected void writeMarshallable2(BytesOut<?> bytes) {
    }

    @Override
    public int intMessageType() {
        return MessageTypes.CLUSTER_STATUS_RESPONSE;
    }
}
