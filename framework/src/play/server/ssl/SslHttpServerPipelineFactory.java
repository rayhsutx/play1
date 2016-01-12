package play.server.ssl;

import java.util.ArrayList;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

import play.Logger;
import play.Play;
import play.server.FlashPolicyHandler;
import play.server.StreamChunkAggregator;
import static org.jboss.netty.channel.Channels.pipeline;


public class SslHttpServerPipelineFactory implements ChannelPipelineFactory {

    public ChannelPipeline getPipeline() throws Exception {

        Integer max = Integer.valueOf(Play.configuration.getProperty("play.netty.maxContentLength", "-1"));
        String mode = Play.configuration.getProperty("play.netty.clientAuth", "none");
        //by default enable only TLS 1, 1.1 and 1.2, possible to override in application.conf
        String enabledProtocols = Play.configuration.getProperty("play.ssl.enabledProtocols", "TLSv1,TLSv1.1,TLSv1.2");

        ChannelPipeline pipeline = pipeline();

        // Add SSL handler first to encrypt and decrypt everything.
        SSLEngine engine = SslHttpServerContextFactory.getServerContext().createSSLEngine();
        engine.setUseClientMode(false);

        //Enable protocols based on configuration
        String[] enabledProtocolsArray = enabledProtocols.split(",");
        String[] supportedProtocols = engine.getSupportedProtocols();
        ArrayList<String> protocols = new ArrayList<String>();
        if (enabledProtocolsArray != null && enabledProtocolsArray.length > 0)
        {
        	for (String protocol : enabledProtocolsArray)
        	{
        		for (String supported : supportedProtocols)
        		{
        			if (supported.equals(protocol))
        				protocols.add(protocol);
        		}
        	}
        }
        if (protocols.size() > 0)
        	engine.setEnabledProtocols(protocols.toArray(new String[0]));

        if ("want".equalsIgnoreCase(mode)) {
            engine.setWantClientAuth(true);
        } else if ("need".equalsIgnoreCase(mode)) {
            engine.setNeedClientAuth(true);
        }
        
        engine.setEnableSessionCreation(true);

        pipeline.addLast("flashPolicy", new FlashPolicyHandler());
        pipeline.addLast("ssl", new SslHandler(engine));
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new StreamChunkAggregator(max));
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

        pipeline.addLast("handler", new SslPlayHandler());

        return pipeline;
    }
}

