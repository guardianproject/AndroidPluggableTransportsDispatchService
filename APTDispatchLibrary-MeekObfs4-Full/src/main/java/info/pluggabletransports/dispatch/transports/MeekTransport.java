package info.pluggabletransports.dispatch.transports;

import android.content.Context;
import android.util.Log;


import com.runjva.sourceforge.jsocks.protocol.Socks4Proxy;
import com.runjva.sourceforge.jsocks.protocol.SocksSocket;

import goptbundle.Goptbundle;
import info.pluggabletransports.dispatch.Connection;
import info.pluggabletransports.dispatch.DispatchConstants;
import info.pluggabletransports.dispatch.Dispatcher;
import info.pluggabletransports.dispatch.Listener;
import info.pluggabletransports.dispatch.Transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;

import static info.pluggabletransports.dispatch.DispatchConstants.PT_TRANSPORTS_MEEK;
import static info.pluggabletransports.dispatch.DispatchConstants.TAG;

public class MeekTransport implements Transport {

    public final static String OPTION_FRONT = "front";
    public final static String OPTION_KEY = "key";
    public final static String OPTION_URL = "url";

    private final static int DEFAULT_MEEK_SOCKS_PORT = 47352;

    private String mPtStateDir;
    private String mMeekFrontDomain;
    private String mMeekKey;
    private String mMeekUrl;

    @Override
    public void register() {
        Dispatcher.get().register(PT_TRANSPORTS_MEEK, getClass());
    }

    @Override
    public void init(Context context, Properties options) {

        initMeekSharedLibrary(context);

        mPtStateDir = context.getDir("pt-state", Context.MODE_PRIVATE).getAbsolutePath();

        mMeekFrontDomain = options.getProperty(OPTION_FRONT);
        mMeekKey = options.getProperty(OPTION_KEY);
        mMeekUrl = options.getProperty(OPTION_URL);
    }

    @Override
    public Connection connect(String addr) {
        Goptbundle.load(mPtStateDir);

        try {
            return new MeekConnection(addr, InetAddress.getLocalHost(), DEFAULT_MEEK_SOCKS_PORT);
        } catch (IOException e) {
            Log.e(getClass().getName(),"Error making connection",e);
            return null;
        }
    }


    @Override
    public Listener listen(String addr) {
        return null;
    }

    private void initMeekSharedLibrary(Context context) {

        try {
            Goptbundle.setenv(DispatchConstants.TOR_PT_LOG_LEVEL, "DEBUG");
            Goptbundle.setenv(DispatchConstants.TOR_PT_CLIENT_TRANSPORTS, "meek_lite");
            Goptbundle.setenv(DispatchConstants.TOR_PT_MANAGED_TRANSPORT_VER, "1");
            Goptbundle.setenv(DispatchConstants.TOR_PT_EXIT_ON_STDIN_CLOSE, "0");
        } catch (Exception e) {
            Log.e(getClass().getName(), "Error setting env variables", e);
        }

    }

    class MeekConnection implements Connection {

        private InetAddress mLocalAddress;
        private int mLocalPort;

        public MeekConnection(String bridgeAddr, InetAddress localSocks, int port) throws IOException {
            //init connection to local socks port
            mMeekUrl = bridgeAddr;
            mLocalAddress = localSocks;
            mLocalPort = port;

            initBridgeViaSocks();
        }

        private void initBridgeViaSocks() throws IOException {
            //connect to SOCKS port and pass the values appropriately to configure meek
            //see: https://gitweb.torproject.org/torspec.git/tree/pt-spec.txt#n628

            StringBuffer meekConfig = new StringBuffer();

            meekConfig.append(OPTION_URL).append("=").append(mMeekUrl).append(";");
            meekConfig.append(OPTION_FRONT).append("=").append(mMeekFrontDomain).append(";");
            meekConfig.append(OPTION_KEY).append("=").append(mMeekKey).append(";");

             Socks4Proxy proxy = new Socks4Proxy("127.0.0.1",DEFAULT_MEEK_SOCKS_PORT,meekConfig.toString());
              SocksSocket s = new SocksSocket(proxy, "www.torproject.org",80);
             InputStream is = s.getInputStream();

            /**
             * 3.5. Pluggable Transport Client Per-Connection Arguments

             Certain PT transport protocols require that the client provides
             per-connection arguments when making outgoing connections.  On
             the server side, this is handled by the "ARGS" optional argument
             as part of the "SMETHOD" message.

             On the client side, arguments are passed via the authentication
             fields that are part of the SOCKS protocol.

             First the "<Key>=<Value>" formatted arguments MUST be escaped,
             such that all backslash, equal sign, and semicolon characters
             are escaped with a backslash.

             Second, all of the escaped are concatenated together.

             Example:

             shared-secret=rahasia;secrets-file=/tmp/blob

             Lastly the arguments are transmitted when making the outgoing
             connection using the authentication mechanism specific to the
             SOCKS protocol version.

             - In the case of SOCKS 4, the concatenated argument list is
             transmitted in the "USERID" field of the "CONNECT" request.

             - In the case of SOCKS 5, the parent process must negotiate
             "Username/Password" authentication [RFC1929], and transmit
             the arguments encoded in the "UNAME" and "PASSWD" fields.

             If the encoded argument list is less than 255 bytes in
             length, the "PLEN" field must be set to "1" and the "PASSWD"
             field must contain a single NUL character.
             */
        }

        /**
         * Read from socks socket
         *
         * @param b
         * @param offset
         * @param length
         * @return
         * @throws IOException
         */
        @Override
        public int read(byte[] b, int offset, int length) throws IOException {
            return 0;
        }

        /**
         * Write to socks socket
         *
         * @param b
         * @throws IOException
         */
        @Override
        public void write(byte[] b) throws IOException {

        }

        /**
         * Close socks socket
         */
        @Override
        public void close() {
            Goptbundle.close();
        }

        @Override
        public InetAddress getLocalAddress() {
            return mLocalAddress;
        }

        @Override
        public int getLocalPort() {
            return mLocalPort;
        }

        @Override
        public InetAddress getRemoteAddress() {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public void setDeadline(Date deadlineTime) {

        }

        @Override
        public void setReadDeadline(Date deadlineTime) {

        }

        @Override
        public void setWriteDeadline(Date deadlineTime) {

        }
    }
}
