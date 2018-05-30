package org.andreyko.vertx.grpc.test

import com.satori.libs.vertx.kotlin.*

class GtfsLookupService : GtfsLookupGrpc.GtfsLookupVertxImplBase() {
  override fun ping(request: PingRequest, future: VxFuture<PingReply>) {
    future.complete(PingReply.newBuilder().apply {
      message = request.message
    }.build())
  }
}