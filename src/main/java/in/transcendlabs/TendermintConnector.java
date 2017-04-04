package in.transcendlabs;

import com.github.jtendermint.jabci.api.ICheckTx;
import com.github.jtendermint.jabci.api.ICommit;
import com.github.jtendermint.jabci.api.IDeliverTx;
import com.github.jtendermint.jabci.socket.TSocket;
import com.github.jtendermint.jabci.types.Types;
import com.github.jtmsp.websocket.ByteUtil;
import com.github.jtmsp.websocket.Websocket;
import com.github.jtmsp.websocket.WebsocketStatus;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;

import javax.websocket.CloseReason;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by sganesan on 4/4/17.
 */
public class TendermintConnector implements IDeliverTx, ICheckTx, ICommit, WebsocketStatus {

    private Websocket wsClient;

    private TSocket socket;

    private Gson gson = new Gson();

    private int hashcount = 0;

    ScheduledExecutorService executorService;

    private List<String> words;

    public TendermintConnector() {
        wsClient = new Websocket(this);
        socket = new TSocket();
        socket.registerListener(this);
        new Thread(socket::start).start();

        words = new ArrayList<>();

        executorService = Executors.newScheduledThreadPool(1);
        executorService.schedule(() -> reconnectWS(), 10, TimeUnit.SECONDS);
    }

    private void reconnectWS() {
        System.out.println("Trying to connect to Websocket...");
        wsClient.reconnectWebsocket();
    }

    private boolean isValidWord(String word) {
        int len = words.size();
        boolean valid = false;
        for (int i=0;i<word.length();i++) {
            if (word.charAt(i) >= 'a' && word.charAt(i) <= 'z') {
                // ok
            } else {
                return false;
            }
        }
        if (words.size() == 0) {
            valid = true;
        } else {
            String lastWord = words.get(len - 1);
            if (word.charAt(0) == lastWord.charAt(lastWord.length() - 1)) {
                valid = true;
            }
        }
        return valid;
    }

    @Override
    public Types.ResponseCheckTx requestCheckTx(Types.RequestCheckTx requestCheckTx) {
        String word = new String(requestCheckTx.getTx().toByteArray());
        if (isValidWord(word)) {
            return Types.ResponseCheckTx.newBuilder().setCode(Types.CodeType.OK).build();
        } else {
            return Types.ResponseCheckTx.newBuilder().setCode(Types.CodeType.BaseInvalidSequence).setLog("Word does not match the required specifications. 1. Word should be lower case English alphabets. 2. Word should start with last character of the previous word.").build();
        }
    }

    @Override
    public Types.ResponseCommit requestCommit(Types.RequestCommit requestCommit) {
        hashcount++;
        return Types.ResponseCommit.newBuilder().setCode(Types.CodeType.OK).setData(ByteString.copyFrom(ByteUtil.toBytes(hashcount))).build();
    }

    @Override
    public Types.ResponseDeliverTx receivedDeliverTx(Types.RequestDeliverTx requestDeliverTx) {
        String word = new String(requestDeliverTx.getTx().toByteArray());
        if (isValidWord(word)) {
            words.add(word);
            return Types.ResponseDeliverTx.newBuilder().setCode(Types.CodeType.OK).build();
        } else {
            String lastWord = null;
            if (words.size() > 0) {
                lastWord = words.get(words.size() - 1);
            }
            return Types.ResponseDeliverTx.newBuilder().setCode(Types.CodeType.BaseInvalidSequence).setLog("Word does not match the required specifications. 1. Word should be lower case English alphabets. 2. Word should start with last character of the previous word. {last word: " + lastWord + "}" ).build();
        }
    }

    @Override
    public void wasClosed(CloseReason cr) {
        if (!"Manual Close".equals(cr.getReasonPhrase())) {
            System.out.println("Websocket closed... reconnecting");
            reconnectWS();
        }
    }
}
