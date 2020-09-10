/* 2012-05-20 Version 2.0

// Daniel Steed
// Date: 7/8/2020




TO EXECUTE:

1. Start the HostServer in some shell. >> java HostServer

1. start a web browser and point it to http://localhost:4242. Enter some text and press
the submit button to simulate a state-maintained conversation.

2. start a second web browser, also pointed to http://localhost:4242 and do the same. Note
that the two agents do not interfere with one another.

3. To suggest to an agent that it migrate, enter the string "migrate"
in the text box and submit. The agent will migrate to a new port, but keep its old state.

During migration, stop at each step and view the source of the web page to see how the
server informs the client where it will be going in this stateless environment.

-----------------------------------------------------------------------------------

COMMENTS:

Hosts agents which can migrate from a server:port to a different server:port combination. We can observe and verify the migration
in our webBrowser.  For instance after we migrate or make a new request we can see the port number changing after localhost:

The state is represented as an integer and is incremented with new requests.

The example uses a standard, default, HostListener port of 4242.

-----------------------------------------------------------------------------------

DESIGN OVERVIEW

Here is the high-level design, more or less:

HOST SERVER
  Runs on some machine
  Port counter is just a global integer incrememented after each assignment
  Loop:
    Accept connection with a request for hosting
    Spawn an Agent Looper/Listener with the new, unique, port

AGENT LOOPER/LISTENER
  Make an initial state, or accept an existing state if this is a migration
  Get an available port from this host server
  Set the port number back to the client which now knows IP address and port of its
         new home.
  Loop:
    Accept connections from web client(s)
    Spawn an agent worker, and pass it the state and the parent socket blocked in this loop

AGENT WORKER
  If normal interaction, just update the state, and pretend to play the animal game
  (Migration should be decided autonomously by the agent, but we instigate it here with client)
  If Migration:
    Select a new host
    Send server a request for hosting, along with its state
    Get back a new port where it is now already living in its next incarnation
    Send HTML FORM to web client pointing to the new host/port.
    Wake up and kill the Parent AgentLooper/Listener by closing the socket
    Die

WEB CLIENT
  Just a standard web browser pointing to http://localhost:4242 to start.

  -------------------------------------------------------------------------------*/


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * AgentWorker
 *
 * AgentWorker objects are created by AgentListeners and process requests made at the various
 * active ports occupied by agentlistener objects. They take a request and look for the string
 * migrate in that request(supplied from a get parameter via an html form). If migrate is found,
 * the worker finds the next availabel port and switches teh client to it.
 *
 *
 */
class AgentWorker extends Thread {

    Socket sock;
    agentHolder parentAgentHolder;
    int localPort;

    AgentWorker (Socket s, int prt, agentHolder ah) {
        sock = s;
        localPort = prt;
        parentAgentHolder = ah;
    }
    public void run() {


        PrintStream out = null;
        BufferedReader in = null;

        String NewHost = "localhost";

        int NewHostMainPort = 4242;
        String buf = "";
        int newPort;
        Socket clientSock;
        BufferedReader fromHostServer;
        PrintStream toHostServer;

        try {
            out = new PrintStream(sock.getOutputStream());
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));


            String inLine = in.readLine();

            StringBuilder htmlString = new StringBuilder();


            System.out.println();
            System.out.println("Request line: " + inLine);

            if(inLine.indexOf("migrate") > -1) {



                clientSock = new Socket(NewHost, NewHostMainPort);
                fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));

                toHostServer = new PrintStream(clientSock.getOutputStream());
                toHostServer.println("Please host me. Send my port! [State=" + parentAgentHolder.agentState + "]");
                toHostServer.flush();


                for(;;) {

                    buf = fromHostServer.readLine();
                    if(buf.indexOf("[Port=") > -1) {
                        break;
                    }
                }


                String tempbuf = buf.substring( buf.indexOf("[Port=")+6, buf.indexOf("]", buf.indexOf("[Port=")) );
                newPort = Integer.parseInt(tempbuf);

                System.out.println("newPort is: " + newPort);


                htmlString.append(AgentListener.sendHTMLheader(newPort, NewHost, inLine));

                htmlString.append("<h3>We are migrating to host " + newPort + "</h3> \n");
                htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n");

                htmlString.append(AgentListener.sendHTMLsubmit());


                System.out.println("Killing parent listening loop.");

                ServerSocket ss = parentAgentHolder.sock;

                ss.close();


            } else if(inLine.indexOf("person") > -1) {

                parentAgentHolder.agentState++;

                htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
                htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n");
                htmlString.append(AgentListener.sendHTMLsubmit());

            } else {

                htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
                htmlString.append("You have not entered a valid request!\n");
                htmlString.append(AgentListener.sendHTMLsubmit());


            }

            AgentListener.sendHTMLtoStream(htmlString.toString(), out);


            sock.close();


        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

}

/**
 * Basic agent holder object. Holds state info/resources
 * so we can track the agentState and pass it between ports
 */
class agentHolder {
    ServerSocket sock;
    int agentState;


    agentHolder(ServerSocket s) { sock = s;}
}
/**
 * AgentListener objects watch individual ports and respond to requests
 * made upon them(in this scenario from a standard web browser); Created
 * by the hostserver when a new request is made to 4242
 *
 */
class AgentListener extends Thread {

    Socket sock;
    int localPort;


    AgentListener(Socket As, int prt) {
        sock = As;
        localPort = prt;
    }

    int agentState = 0;


    public void run() {
        BufferedReader in = null;
        PrintStream out = null;
        String NewHost = "localhost";
        System.out.println("In AgentListener Thread");
        try {
            String buf;
            out = new PrintStream(sock.getOutputStream());
            in =  new BufferedReader(new InputStreamReader(sock.getInputStream()));


            buf = in.readLine();


            if(buf != null && buf.indexOf("[State=") > -1) {

                String tempbuf = buf.substring(buf.indexOf("[State=")+7, buf.indexOf("]", buf.indexOf("[State=")));
                //parse it
                agentState = Integer.parseInt(tempbuf);

                System.out.println("agentState is: " + agentState);

            }

            System.out.println(buf);

            StringBuilder htmlResponse = new StringBuilder();


            htmlResponse.append(sendHTMLheader(localPort, NewHost, buf));
            htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n");
            htmlResponse.append("[Port="+localPort+"]<br/>\n");
            htmlResponse.append(sendHTMLsubmit());

            sendHTMLtoStream(htmlResponse.toString(), out);


            ServerSocket servsock = new ServerSocket(localPort,2);

            agentHolder agenthold = new agentHolder(servsock);
            agenthold.agentState = agentState;


            while(true) {
                sock = servsock.accept();

                System.out.println("Got a connection to agent at port " + localPort);

                new AgentWorker(sock, localPort, agenthold).start();
            }

        } catch(IOException ioe) {

            System.out.println("Either connection failed, or just killed listener loop for agent at port " + localPort);
            System.out.println(ioe);
        }
    }




    static String sendHTMLheader(int localPort, String NewHost, String inLine) {

        StringBuilder htmlString = new StringBuilder();

        htmlString.append("<html><head> </head><body>\n");
        htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + NewHost + "</h2>\n");
        htmlString.append("<h3>You sent: "+ inLine + "</h3>");
        htmlString.append("\n<form method=\"GET\" action=\"http://" + NewHost +":" + localPort + "\">\n");
        htmlString.append("Enter text or <i>migrate</i>:");
        htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n");

        return htmlString.toString();
    }

    static String sendHTMLsubmit() {
        return "<input type=\"submit\" value=\"Submit\"" + "</p>\n</form></body></html>\n";
    }


    static void sendHTMLtoStream(String html, PrintStream out) {

        out.println("HTTP/1.1 200 OK");
        out.println("Content-Length: " + html.length());
        out.println("Content-Type: text/html");
        out.println("");
        out.println(html);
    }

}
/**
 *
 * main hostserver class. this listens on port 4242 for requests. at each request,
 * increment NextPort and start a new listener on it. Assumes that all ports >3000
 * are free.
 */
public class HostServer {

    public static int NextPort = 3000;

    public static void main(String[] a) throws IOException {
        int q_len = 6;
        int port = 4242;
        Socket sock;

        ServerSocket servsock = new ServerSocket(port, q_len);
        System.out.println("Elliott/Reagan DIA Master receiver started at port 4242.");
        System.out.println("Connect from 1 to 3 browsers using \"http:\\\\localhost:4242\"\n");

        while(true) {

            NextPort = NextPort + 1;

            sock = servsock.accept();

            System.out.println("Starting AgentListener at port " + NextPort);
            new AgentListener(sock, NextPort).start();
        }

    }
}
