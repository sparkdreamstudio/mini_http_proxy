/* CS 656 / Fall 2019 / Term Project
 * Group: W5 / Leader: Mohammed (mc837)
 * Group Members: Shourya (sp2685), akul (ap2559), xiang (xl527), rohit (rp879), ashish (ak2633) */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
public class Apache2 {
    private byte[] HOST; /* should be initialized to 1024 bytes in the constructor */
    private int PORT; /* port this Apache should listen on, from cmdline */
    private InetAddress PREFERRED; /* must set this in dns() */
    public static void main(String[] a) {
        try{
            if(Integer.parseInt(a[0])<1025 || Integer.parseInt(a[0])>65535){
                System.out.write("Invalid port number\n".getBytes());
                return;
            }
            Apache2 apache = new Apache2(Integer.parseInt(a[0]));
            apache.run(2);
        }catch(IOException e){}
    }
    public Apache2(int p) {
        this.HOST = new byte[2048];
        this.PORT = p; }
    public int run(int X) throws IOException {   /* you can define the signature for run() */
        ServerSocket server = null;
        Socket client = null;
        InetSocketAddress inetSocketAddress = null;
        try {
            server = new ServerSocket();
            inetSocketAddress = new InetSocketAddress(PORT);
            //int waitQueueSize = X;
            server.bind(inetSocketAddress);
            System.out.write(concatenateByte("Apache Listening on socket ".getBytes(),String.valueOf(PORT).getBytes(),"\n".getBytes()));
            int clientCounter = 0;
            while (true) {
                System.gc();
                PREFERRED=null;
                client = server.accept();
                System.out.write(("(" + (++clientCounter) + ") Incoming client Connection from ["+ client.getInetAddress().getHostAddress() + ":" + client.getPort() + "] to me ["+ client.getLocalAddress().getHostAddress() + ":" + client.getLocalPort()+"]\n").getBytes());              
                int status = 2,requestlen=0,checkCRLF=4;
                byte[] burl = new byte[1024],host = new byte[1024],port = new byte[1024],clientByteArray = new byte[1024],request = new byte[65535],httpversion=new byte[8];
                int pos=0;
                try{do {
                    cleanbyteArray(clientByteArray);
                    int readnum=client.getInputStream().read(clientByteArray,0 ,1024);
                    if(readnum == 0)
                        break;
                    for(int i=0;clientByteArray[i]!=0&&clientByteArray[i]!=-1;++i){
                        readnum=i+1;
                        if(checkCRLF==4&&clientByteArray[i]=='\r'){
                            checkCRLF--;
                        }else if(checkCRLF==3&&clientByteArray[i]=='\n'){
                            checkCRLF--;
                        }else if(checkCRLF==2&&clientByteArray[i]=='\r'){
                            checkCRLF--;
                        }else if(checkCRLF==1&&clientByteArray[i]=='\n'){
                            checkCRLF--;
                            break;}
                        else{checkCRLF=4;}       
                    }
                    requestlen+=readnum;
                    if(requestlen>65535){
                        status=400;
                        break;}
                    pos=copyBytetoByte(clientByteArray, request, pos,readnum); 
                } while (checkCRLF!=0);} catch(IOException e){client.close(); continue;}
                status=parse(request,status,burl,host,port,httpversion);
                if(httpversion[7]=='1'&& host[0]==0) {
                    status =400;}
                if(status ==200)
                    dns(clientByteArray, host,port, client);
                ht_fetch(status,httpversion, host, burl, port, client,request);
                cleanbyteArray(HOST);
                client.close();
            }
        } catch (Exception e) {}
        return 0;}
    int dns(byte[] clientByteArray, byte[] host,byte[] bport, Socket client)throws Exception { /* you can define the signature for dns() */
        PREFERRED = null;
        int port =0;
        for(int i =0;bport[i]!=0;i++)
            port=port*10+(bport[i]-48);
        try {
            if (host[0] == -1) {
                System.out.write("    REQ: Unknown Command / RESP: ERROR\n".getBytes());
            } else {
                char[] strhost=new char[host.length];
                for(int i=0;i <host.length;++i)
                    strhost[i]=(char)host[i];
                InetAddress[] allIps = InetAddress.getAllByName(String.valueOf(strhost));
                for (InetAddress ip : allIps) {
                    if (ip.getClass() == Inet6Address.class)
                        continue;
                    PREFERRED = ip;
                    break;
                }}
        } catch (UnknownHostException e) {} 
        return 0;}
    int ht_fetch(int code,byte[] httpversion, byte[] host, byte[] uri,byte[] port,Socket client,byte[] request) throws IOException{
        try{
            if(code != 200 ){
                if(code == 400)
                {
                    System.out.write(concatenateByte("    REQ: ".getBytes(),host," /  RESP: ERROR 400\n".getBytes()));
                    copyBytetoByte(new byte[]{'H','T','T','P','/','1','.','1',' ','4','0','0',' ','B','a','d',' ','R','e','q','u','e','s','t','\r','\n','\r','\n','<','h','t','m','l','>','<','b','o','d','y','>','<','p','>','4','0','0',' ','B','a','d',' ','R','e','q','u','e','s','t','<','/','p','>','<','/','h','t','m','l','>','<','/','b','o','d','y','>'}, HOST, 0);
                    client.getOutputStream().write(HOST);}
            }
            else{
                if (PREFERRED!=null){
                    int _port = 0;
                    for (int i = 0; port[i] != 0; i++)
                        _port = _port * 10 + (port[i] - 48);
                    int responseLen = 0;
                    int datalen=1024*1024;
                    byte[] temp = new byte[datalen];
                    Socket target = new Socket();
                    int incomeBytes=0;
                    try{
                        target.connect(new InetSocketAddress(PREFERRED, _port));
                        target.getOutputStream().write(request);
                        while((incomeBytes = target.getInputStream().read(temp))!=-1){
                            client.getOutputStream().write(temp,0,incomeBytes);
                            responseLen += incomeBytes;}}
                    catch(IOException e){
                        if(e.getMessage().equals("Connection refused (Connection refused)")){
                            errorHostOrPort(client," 200 OK".getBytes(), "Connection refused".getBytes(), host, uri, port);
                            target.close();
                            return 0;
                        }
                    }
                    System.out.write(concatenateByte("    REQ: ".getBytes(),host," /  RESP:  (".getBytes(),String.valueOf(responseLen).getBytes()," bytes transferred.)\n".getBytes()));
                    target.close();
                }
                else if (host[0]=='l'&&host[1]=='o'&&host[2]=='c'&&host[3]=='a'&&host[4]=='l'&&host[5]=='f'&&host[6]=='i'&&host[7]=='l'&&host[8]=='e'&&host[9]==0){
                    //System.out.println(System.getProperty("user.dir"));
                    File file = new File(new String(concatenateByte(System.getProperty("user.dir").getBytes(),uri)));
                    if(!file.exists()){
                        errorHostOrPort(client," 404 Not Found".getBytes(), "Page Not Found".getBytes(), host, uri, port);}
                    else{
                        int responseLen = 0,datalen=1024*1024,incomeBytes=0;
                        byte[] temp = new byte[datalen];
                        FileInputStream fr = new FileInputStream(file);
                        client.getOutputStream().write(new byte[]{'H','T','T','P','/','1','.','1',' ','2','0','0',' ','O','K','\r','\n','\r','\n'});
                        while ((incomeBytes=fr.read(temp, 0, datalen)) != -1){
                            client.getOutputStream().write(temp,0,incomeBytes);responseLen += incomeBytes;}
                        fr.close();
                        System.out.write(concatenateByte("    REQ: ".getBytes(),host," /  RESP:  (".getBytes(),String.valueOf(responseLen).getBytes()," bytes transferred.)\n".getBytes()));
                    }
                }
                else{
                    errorHostOrPort(client," 200 OK".getBytes(), "Unknown Host".getBytes(), host, uri, port);
                }
            }}
        catch(IOException e){}
        return 0;
    }
    void errorHostOrPort(Socket client,byte[] codemsg, byte[] msg, byte[] host, byte[] uri,byte[] port)throws IOException {
        System.out.write(concatenateByte("    REQ: ".getBytes(), host, " /  RESP: ERROR (".getBytes(),msg,")\n".getBytes()));
        int pos = copyBytetoByte(concatenateByte(new byte[]{'H','T','T','P','/','1','.','1',' '},codemsg,new byte[]{'\r','\n','\r','\n'}), HOST, 0);
        pos=copyBytetoByte(new byte[]{'<','h','t','m','l','>','<','b','o','d','y','>'}, HOST, pos);
        pos = addHtmlLine(msg,true, pos);
        pos = copyBytetoByte(new byte[] { '<', '/', 'h', 't', 'm', 'l', '>', '<', '/', 'b', 'o', 'd', 'y', '>' }, HOST,pos);
        client.getOutputStream().write(HOST);
    }
    int parse(byte[] buffer,int status,byte[] path,byte[] host,byte[] port,byte[] httpversion){
        byte[] line=new byte[2500];
        int stcode=status;
        int lineend=find(0,buffer,(byte)'\n'),start =0;
        copybyte(buffer,line,start,lineend+1);
        while(line[0]!=0){
            if(line[0]=='\r'&&line[1]=='\n'&&stcode==1)
                return 200;
            if(stcode==2){
                if(line[0]!='G'||line[1]!='E'||line[2]!='T'||line[3]!=' ')
                    return 400;
                int nextspace=find(4, line, (byte)' ');
                if(nextspace==-1)
                    return 400;
                byte[] uri=new byte[2048];
                copybyte(line, uri, 4,nextspace-4);
                if(uri[0]=='/')
                    copybyte(uri,path,0,find(0, uri, (byte)0));
                else if(uri[0]=='h'&&uri[1]=='t'&&uri[2]=='t'&&uri[3]=='p'&&uri[4]==':'&&uri[5]=='/'&&uri[6]=='/'){
                    byte[] newuri=new byte[2048];
                    copybyte(uri, newuri, 7,find(0, uri, (byte)0));
                    parseuri(newuri, host, path, port);
                }
                else
                    parseuri(uri, host, path, port);
                if(line[nextspace+1]!='H'||line[nextspace+2]!='T'||line[nextspace+3]!='T'||line[nextspace+4]!='P'||line[nextspace+5]!='/'||line[nextspace+6]!='1'||line[nextspace+7]!='.'||(line[nextspace+8]!='0'&&line[nextspace+8]!='1')||line[nextspace+9]!='\r'||line[nextspace+10]!='\n')
                    return 400;
                copybyte(line, httpversion, nextspace+1, 8);  
                --stcode;
            }
            else if(stcode==1){
                int colonIndex= find(0,line,(byte)':');
                if(colonIndex==-1 ||colonIndex==find(0,line,(byte)'\r')-1)
                    return 400;
                byte[] copy=new byte[find(0,line,(byte)0)-2];
                copybyte(line, copy, 0, copy.length);
                if((line[0]=='H'&&line[1]=='o'&&line[2]=='s'&&line[3]=='t')){
                    cleanbyteArray(host);
                    copybyte(line, host, colonIndex+1, find(0,line,(byte)'\r')-colonIndex-1);
                    byte[] uri=new byte[2048];
                    byte[] pathholder=new byte[20];
                    if(line[colonIndex+1]==' ')
                        copybyte(line, uri, colonIndex+2, find(0,line,(byte)'\r')-colonIndex-2);
                    else
                        copybyte(line, uri, colonIndex+1, find(0,line,(byte)'\r')-colonIndex-1);
                    parseuri(uri, host, pathholder, port);
                }
            }
            cleanbyteArray(line);
            start = lineend+1;
            lineend=find(start,buffer,(byte)'\n');
            copybyte(buffer,line,start,lineend-start+1);
        }
        return stcode;
    }
    void parseuri(byte[] uri, byte[] host, byte[] path, byte[] port) {
        int slashstart = find(0, uri, (byte) '/'),clonindex = find(0, uri, (byte) ':');
        cleanbyteArray(host);cleanbyteArray(path);cleanbyteArray(port);
        if (slashstart == -1) {
            if (clonindex > 0) {
                copybyte(uri, port, clonindex+1, find(clonindex, uri, (byte) 0)-clonindex-1);
                copybyte(uri, host, 0, clonindex);
            } else {
                copybyte(uri, host, 0, find(0, uri, (byte) 0));
                port[0]=(byte)'8';port[1]=(byte)'0';
            }
            path[0] = '/';
        } else {
            if (clonindex > 0) {
                copybyte(uri, port, clonindex+1, slashstart-clonindex-1);
                copybyte(uri, host, 0, clonindex );
            } else {
                copybyte(uri, host, 0, slashstart) ;
                port[0]=(byte)'8';port[1]=(byte)'0';
            }
            copybyte(uri, path, slashstart, find(slashstart, uri, (byte) 0) - slashstart);
        }
    }
    void cleanbyteArray(byte[] buffer){
        for(int i=0;i<buffer.length;i++)
            buffer[i]=0;
    }
    void copybyte(byte[] src,byte[] des,int start,int len){
        for(int i=start;i-start<len;i++)
            des[i-start]=src[i];}
    int find(int start,byte[] array,byte val){
        int len = array.length;
        for(int i = start;i < len;++i)
            if(array[i]==val)
                return i;
        return -1;}
    byte[] concatenateByte(byte[]... datas){
        byte[] data;
        int len=0;
        for(int i = 0;i<datas.length;i++){
            if(find(0,datas[i],(byte)0)==-1)
                len+=datas[i].length;
            else
                len+=find(0,datas[i],(byte)0);
        }
        data = new byte[len];
        int start=0;
        for(int i=0;i<datas.length;i++){
            int l=0;
            if(find(0,datas[i],(byte)0)==-1)
                l=datas[i].length;
            else
                l=find(0,datas[i],(byte)0);
            start=copyBytetoByte(datas[i], data, start,l);}
        return data;
    }
    int addHtmlLine(byte[] data,boolean red,int pos){
        int newpos;
        if (red == true)
            newpos=copyBytetoByte(concatenateByte(new byte[]{'<','p',' ','s','t','y','l','e','=',34,'c','o','l','o','r',':','r','e','d',';',34,'>'},data,new byte[]{'<','/','p','>'}),HOST,pos);
        else 
            newpos=copyBytetoByte(concatenateByte(new byte[]{'<','p','>'},data,new byte[]{'<','/','p','>'}),HOST,pos);
        return newpos;
    }
    int copyBytetoByte(byte[] source, byte[] dest, int pos) {// wrap the data to the host,// return the index of data which should be written next
        for (int start = pos; (pos - start) < source.length && source[pos - start] != 0&& source[pos - start] != -1; ++pos)
            dest[pos] = source[pos - start];
        return pos;}
    int copyBytetoByte(byte[] source, byte[] dest, int pos,int len) {// wrap the data to the host,// return the index of data which should be written next
        for (int start = pos; (pos - start) < len && source[pos - start] != 0&& source[pos - start] != -1; ++pos)
            dest[pos] = source[pos - start];
        return pos;}
}