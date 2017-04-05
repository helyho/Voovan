package org.voovan.test.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.voovan.tools.TByteBuffer;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class NettyServer {

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new DiscardServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(28080).sync();

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static class DiscardServerHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            //byte[] buffer = new byte[((ByteBuf) msg).readableBytes()];
            //((ByteBuf) msg).readBytes(buffer);
            //System.out.println(new String(buffer));
            //((ByteBuf) msg).clear();
            ((ByteBuf) msg).release();
            String retVal = "HTTP/1.1 200 OK\r\n" +
                    "Server: Voovan-WebServer/V1.0-RC-1\r\n" +
                    "Connection: keep-alive\r\n" +
                    "Content-Length: 2\r\n" +
                    "Date: Thu, 05 Jan 2017 04:55:20 GMT\r\n" +
                    "Content-Type: text/html\r\n"+
                    "\r\n"+
                    "OK\r\n\r\n";
            ByteBuf bf = ctx.alloc().buffer(retVal.length());
            bf.writeBytes(retVal.getBytes());
            ctx.writeAndFlush(bf);
            ctx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
            // Close the connection when an exception is raised.
            cause.printStackTrace();
            ctx.close();
        }
    }
}
