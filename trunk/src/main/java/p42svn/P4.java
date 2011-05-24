package p42svn;

import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;

/**
 * @author Pavel Belevich
 *         Date: 5/7/11
 *         Time: 6:18 PM
 */
public class P4 {

    private IServer server;

    private String serverUriString;
    private String userName;
    private String password;
    private String clientName;

    public String getServerUriString() {
        return serverUriString;
    }

    public void setServerUriString(String serverUriString) {
        this.serverUriString = serverUriString;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public IServer getServer(boolean check) throws P4Exception {
        try {
            if (server == null) {
                server = ServerFactory.getServer(serverUriString, null);
            }
            if (check) {
                if (!server.isConnected()) {
                    server.connect();
                }
                if (server.getLoginStatus().contains("Perforce password (P4PASSWD) invalid or unset.") ||
                        server.getLoginStatus().contains("Access for user 'nouser' has not been enabled by 'p4 protect'.")) {
                    server.setUserName(userName);
                    server.login(password);
                    //TODO Is it required?
//                IClient client = server.getClient(clientName);
//                if (client != null) {
//                    server.setCurrentClient(client);
//                }
                }
            }
            return server;
        } catch (Exception e) {
            throw new P4Exception(e);
        }
    }

    public IServer getServer() throws P4Exception {
        return getServer(false);
    }

}
