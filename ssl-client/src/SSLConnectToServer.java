import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

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

/**
 * This class handles and establishes an SSL connection to a server
 */
public class SSLConnectToServer
{
    /*
    Name of key store file
     */
    private final String KEY_STORE_NAME =  "clientkeystore";
    /*
    Password to the key store file
     */
    private final String KEY_STORE_PASSWORD = "storepass";
    private SSLSocketFactory sslSocketFactory;
    private SSLSocket sslSocket;
    private BufferedReader is;
    private PrintWriter os;

    protected String serverAddress;
    protected int serverPort;

    private Socket client;


    public void Create_Key_Store() throws Exception{

    	// Create keystore instance
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

		// Load the empty keystore
		ks.load(null, null);

		// Initialize the certificate factory
		CertificateFactory cf = CertificateFactory.getInstance("X.509");

		// Load the certificate
		InputStream caInput = new BufferedInputStream(new FileInputStream(new File("server_crt.crt")));

		Certificate ca;
		try {
			ca = cf.generateCertificate(caInput);
			// System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
		} finally {
			caInput.close();
		}

		// Create the certificate entry
		ks.setCertificateEntry("ca", ca);

		// Write the file back
		ks.store(new FileOutputStream("clientkeystore"), KEY_STORE_PASSWORD.toCharArray());

    }

    public SSLConnectToServer(String address, int port) throws Exception
    {

        this.serverAddress = address;
        if(port > 65535){
            System.out.println("[CLIENT][ERR]:Port number cannot be larger than the maximum number of ports which is 65535.");
            this.serverPort = port % 65535;
        }

        
        /*
        Loads the keystore's address of client
         */
        System.setProperty("javax.net.ssl.trustStore", KEY_STORE_NAME);

        // Loads the keystore's password of client
        System.setProperty("javax.net.ssl.trustStorePassword", KEY_STORE_PASSWORD);

        ConnectToTCP();
        CrtTransfer();
        DisconnectFromTCP();
        // Load the certificates in the key store
        Create_Key_Store();
    }
    public void ConnectToTCP(){
        try{
            this.client = new Socket(this.serverAddress, 5555);
            System.out.println("[CLIENT][LOG]: Connection establised with the TCP Server.");
        }catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void CrtTransfer(){
        Verification();
        byte[] contents = new byte[1000];

        try{
            BufferedOutputStream osTcp = new BufferedOutputStream(new FileOutputStream("server_crt.crt"));
            InputStream isTcp = this.client.getInputStream();
            System.out.println("[CLIENT][LOG]: Starting certificate transfer.");
            int readBytes = 0;

            while((readBytes = isTcp.read(contents)) != -1){
                osTcp.write(contents, 0, readBytes);
            }
            osTcp.flush();
            System.out.println("[CLIENT][LOG]: Certificate transfer done.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void Verification(){
        String msg = new String();
        String svMsg = new String();
        try{
            this.is=new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            this.os= new PrintWriter(this.client.getOutputStream(), true);
            Scanner sc = new Scanner(System.in);

            while(true){
                svMsg = this.is.readLine();
                if(svMsg.equals("400") || svMsg.equals("200")){
                    if(svMsg.equals("400")) {
                        System.out.println("[CLIENT][ERR]: Client verfication failed.");
                        break;
                    }else{
                        System.out.println("[CLIENT][SUCC]: Client verfication success.");
                        break;
                    }
                }else{
                    System.out.println("[Server][RES]: " + svMsg);
                }
                msg = sc.nextLine();
                this.os.println(msg);
                this.os.flush();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void DisconnectFromTCP(){
        try{
             this.client.close();
             System.out.println("[CLIENT][LOG]Disconneted from TCP server.");
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public void decodeMessages(){
        String[] msg = new String[2];
        String svMsg = new String();
        int index;

        try {
            while((svMsg = is.readLine()) != null){
                for(int i = 0; i < svMsg.length(); i++){
                    index = i%2;
                    if(i < 2) msg[i] = " ";
                    if(svMsg.charAt(i) != ' ') {
                        switch(index){
                            case 0:
                                msg[0] += svMsg.charAt(i);
                                break;
                            case 1:
                                msg[1] += svMsg.charAt(i);
                                break;
                        }
                    }
                }
            }
            System.out.println(msg[0] + "\n" + msg[1] + "\n");
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * Connects to the specified server by serverAddress and serverPort
     */
    public void Connect()
    {
        try
            {
                sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                sslSocket = (SSLSocket) sslSocketFactory.createSocket(serverAddress, serverPort);
                sslSocket.startHandshake();
                is=new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                os= new PrintWriter(sslSocket.getOutputStream());
                System.out.println("[CLIENT][LOG]: Successfully connected to " + serverAddress + " on port " + serverPort);
            }
        catch (Exception e)
            {
                e.printStackTrace();
            }
    }


    /**
     * Disconnects form the specified server
     */
    public void Disconnect()
    {
        try
            {
                is.close();
                os.close();
                sslSocket.close();
            }
        catch (IOException e)
            {
                e.printStackTrace();
            }
    }

    /**
     * Sends a message as a string over the secure channel and receives
     * answer from the server
     * @param message input message
     * @return response from server
     */
    public String SendForAnswer(String message)
    {
        String response = new String();
        try
        {
            os.println(message);
            os.flush();
            response = is.readLine();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            System.out.println("[CLIENT][ERR]: ConnectionToServer. SendForAnswer. Socket read Error");
        }
        return response;
    }


}
