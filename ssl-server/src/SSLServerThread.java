import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

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


public class SSLServerThread extends Thread
{

    private final String SERVER_REPLY = "Hello Client";
    private SSLSocket sslSocket;
    private String line = new String();
    private BufferedReader is;
    private BufferedWriter os;
    public SSLServerThread(SSLSocket s)
    {
        sslSocket = s;
    }

    public void run()
    {
        try
        {
            is = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            os = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()));

        }
        catch (IOException e)
        {
            System.out.println("[SSLSERVER][ERR]: Server Thread. Run. IO error in server thread");
        }

        try
        {
            line = is.readLine();
            os.write(SERVER_REPLY + "\n");
            os.flush();
            System.out.println("[SSLSERVER][SUCC]: Client " + sslSocket.getRemoteSocketAddress() + " sent : " + line);
            sendMessages();


        }
        catch (IOException e)
        {
            line = this.getClass().toString(); //reused String line for getting thread name
            System.out.println("[SSLSERVER][ERR]: Server Thread. Run. IO Error/ Client " + line + " terminated abruptly");
        }
        catch (NullPointerException e)
        {
            line = this.getClass().toString(); //reused String line for getting thread name
            System.out.println("[SSLSERVER][ERR]: Server Thread. Run.Client " + line + " Closed");
        } finally
        {
            try
            {
                System.out.println("[SSLSERVER][LOG]: Closing the connection");
                if (is != null)
                {
                    is.close();
                    System.out.println("[SSLSERVER][LOG]: Socket Input Stream Closed");
                }

                if (os != null)
                {
                    os.close();
                    System.out.println("[SSLSERVER][LOG]: Socket Out Closed");
                }
                if (sslSocket != null)
                {
                    sslSocket.close();
                    System.out.println("[SSLSERVER][LOG]: Socket Closed");
                }

            }
            catch (IOException ie)
            {
                System.out.println("[SSLSERVER][ERR]: Socket Close Error");
            }
        }//end finally
    }
    public void sendMessages(){
        try{
            String msg = new String();
            List<String> info = getInfo();
            int index = 0;
            while(!msg.equals("  ")){
                msg = "";
                for(String s: info){
                    if(index > (s.length()- 1)) msg += " ";
                    else msg += s.charAt(index);
                }

                this.os.write(msg);
                this.os.flush();
                index++;
            }




        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public List<String> getInfo() {

        List<String> lst = new LinkedList<String>();
        String st = new String();

        try {
            BufferedReader emails = new BufferedReader(new FileReader(new File("users.txt")));
            while ((st = emails.readLine()) != null) {
                lst.add(st.substring(0, st.indexOf(":")));
                lst.add(st.substring((st.indexOf(":") + 1)));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return lst;
    }
}
