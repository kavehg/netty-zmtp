This is a ZeroMQ codec for Netty that aims to implement ZMTP, the ZeroMQ
Message Transport Protocol versions 1.0 and 2.0 as specified in
http://rfc.zeromq.org/spec:13 and http://rfc.zeromq.org/spec:15.

This project was originally hosted on https://github.com/spotify/netty-zmtp/
and was based on netty version 3.6.6.

This version is hosted on https://github.com/kavehg/netty-zmtp and is
currently aimed at supporting netty version 5.0

This project implements the ZMTP wire protocol but not the ZeroMQ API, meaning
that it can be used to communicate with other peers using e.g. ZeroMQ (libzmq)
but it's not a drop-in replacement for JZMQ like e.g. JeroMQ attempts to be.

## Usage

To use netty-zmtp, insert one of `ZMTP10Codec` or `ZMTP20Codec` into your
`ChannelPipeline` and it will turn incoming buffers into  `ZMTPIncomingMessage`
instances up the pipeline and accept `ZMTPMessage` instances that gets
serialized into buffers downstream.

## License

This software is licensed using the Apache 2.0 license. Details in the file
LICENSE.txt
