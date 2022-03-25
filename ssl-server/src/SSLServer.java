import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * Copyright [2017] [Yahya Hassanzadeh-Nazarabadi]

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
public class SSLServer extends Thread
{
    private final String KS_FILE = "keystore.jks";
    private final String KS_PASS = "storepass";
    private final String SK_PASS = "keypass";
    private SSLServerSocket sslSocket;
    private SSLServerSocketFactory sslFactory;
    //private ServerControlPanel serverControlPanel;

    //Server Socket for the incoming tcp connections, for certificate transfer.
    private ServerSocket crtServerSocket;
    //Server will always listen to port number 5555 for incoming connections.
    private final int CRTServerPort = 5555;
    //For TLS the server will listen to a given port number;
    private int SSLServerPort;


    public SSLServer(int port)
    {
        if(port > 65535){
            System.out.println("[SERVER][ERR]Port number cannot be larger than the maximum number of ports which is 65535");
            this.SSLServerPort = port % 65535;
        }else System.out.println("[SERVER][SUCC]Port number for the SSL Server is: " + this.SSLServerPort);

        try
        {

            //serverControlPanel = new ServerControlPanel("hello server!");


            /*
            Instance of SSL protocol with TLS variance
             */
            SSLContext sc = SSLContext.getInstance("TLS");

            /*
            Key management of the server
             */
            char ksPass[] = KS_PASS.toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(KS_FILE), ksPass);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, SK_PASS.toCharArray());
            sc.init(kmf.getKeyManagers(), null, null);


            /*
            SSL socket factory which creates SSLSockets
             */
            sslFactory = sc.getServerSocketFactory();
            sslSocket = (SSLServerSocket) sslFactory.createServerSocket(this.SSLServerPort);

            System.out.println("[SERVER][LOG]: SSL server is up and running on port " + this.SSLServerPort);

            this.crtServerSocket = new ServerSocket(this.CRTServerPort);

            System.out.println("[SERVER][LOG]: CRT server is up and running on port " + this.CRTServerPort);

            while (true)
            {
                ListenAndAccept();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    /*
    Listens to the line and starts a connection on receiving a request with the client
     */
    private void ListenAndAccept()
    {
        SSLSocket s;
        //For incoming connections
        Socket clientSocket;
        try
        {
            /*Accepts the incoming connection from port number '5555' and sends the
              client to a crtClient thread to start the certificate transfer.
            */
            clientSocket = crtServerSocket.accept();
            System.out.println("[SERVER][LOG]: A TCP connection was established with a client on the address of " + clientSocket.getRemoteSocketAddress());
            crtClientThread crtTransfer = new crtClientThread(clientSocket);
            crtTransfer.start();

            s = (SSLSocket) sslSocket.accept();
            System.out.println("[SERVER][LOG]: An SSL connection was established with a client on the address of " + s.getRemoteSocketAddress());
            SSLServerThread st = new SSLServerThread(s);
            st.start();

        }

        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("[SERVER][ERR]: Server Class.Connection establishment error inside listen and accept function");
        }
    }
    class crtClientThread extends Thread{
        private Socket client;

        private BufferedInputStream is;
        private OutputStream os;

        private BufferedReader isV;
        private PrintWriter osV;


        public crtClientThread(Socket client){
            this.client = client;
        }

        public void run(){
            if(!verifyClient()){
                try{    
                    System.out.println("[SERVER][ERR]: Closing connection with the client: " + this.client.getRemoteSocketAddress());
                    client.close();
                    return;
                }catch(IOException e){
                    e.printStackTrace();
                }
            }else System.out.println("[SERVER][SUCC]: Client verified: ");

            System.out.println("[SERVER][LOG]: Starting certificate transfer to the client: " + this.client.getRemoteSocketAddress());
            File file = new File("server_crt.crt");

            try{
                this.is = new BufferedInputStream(new FileInputStream(file));
                this.os = this.client.getOutputStream();

                byte[] contents;

                long current = 0;
                long fileSize = file.length();

                while (current != fileSize) {
                    //Packet size
                    int size = 1000;
                    if (fileSize - current >= size) {
                        current += size;
                    } else {
                        size = (int) (fileSize - current);
                        current = fileSize;
                    }
                    contents = new byte[size];

                    is.read(contents);
                    os.write(contents);
                }
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    os.flush();
                    client.close();
                    os.close();
                    is.close();
                    System.out.println("[SERVER][LOG]: File transfer done to the client: " + client.getRemoteSocketAddress());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean verifyClient(){
            boolean check = false;
            String username = new String();
            String password = new String();
            File users = new File("users.txt");

            try{
                this.osV = new PrintWriter(this.client.getOutputStream(), true);
                this.isV = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
                BufferedReader userVer = new BufferedReader(new FileReader(users));
                System.out.println("[SERVER][LOG]: Client waiting to be verified.");
                String line;

                this.osV.println("Username: ");
                username = this.isV.readLine();

                this.osV.println("Password: ");
                password = this.isV.readLine();

                while((line = userVer.readLine()) != null){
                    if (username.equals(line.substring(0, line.indexOf(":")))) {
                        if (password.equals(line.substring((line.lastIndexOf(":") + 1)))) {
                            System.out.println("[SERVER][LOG]: User Verified!");
                            this.osV.println("200");
                            this.osV.flush();
                            check = true;
                        } else {
                            System.out.println("[SERVER][ERR]: Incorrect password! User not verified.");
                            this.osV.println("400");
                            this.osV.flush();
                            check = false;
                        }
                    }else{
                        System.out.println("[SERVER][ERR]: Incorrect username! User not found.");
                        this.osV.println("400");
                        this.osV.flush();
                        check = false;
                    }
                }
                userVer.close();
            }catch(IOException e){
                e.printStackTrace();
            }
            return check;
        }












    }









}





