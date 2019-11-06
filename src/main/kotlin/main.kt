import com.google.common.io.BaseEncoding
import com.google.protobuf.ByteString
import flow.services.observation.Observation
import flow.services.observation.ObserveServiceGrpc
import io.grpc.ManagedChannelBuilder

fun main(args: Array<String>) {

    val managedChannel = ManagedChannelBuilder.forAddress("localhost", 3569).usePlaintext().build()

    val observeService = ObserveServiceGrpc.newBlockingStub(managedChannel);

    val pingResponse = observeService.ping(Observation.PingRequest.newBuilder().build())

    println(pingResponse.address)

    val b = BaseEncoding.base16().decode("01")

    val account = observeService.getAccount(Observation.GetAccountRequest.newBuilder().setAddress(ByteString.copyFrom(b)).build())

    println(account)

}