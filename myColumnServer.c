/*-------------------------------------------------------------------------*
 *---									---*
 *---		columnServer.c						---*
 *---									---*
 *---	    This file defines a C program that gets file-sys commands	---*
 *---	from client via a socket, executes those commands in their own	---*
 *---	threads, and returns the corresponding output back to the	---*
 *---	client.								---*
 *---									---*
 *---	----	----	----	----	----	----	----	----	---*
 *---									---*
 *---									---*
 *-------------------------------------------------------------------------*/

//	Compile with:
//	$ gcc columnServer.c -o columnServer -lpthread

//---		Header file inclusion					---//

#include	"clientServer.h"
#include	<pthread.h>	// For pthread_create()


//---		Definition of constants:				---//

#define		STD_ERROR_MSG		"Error doing operation"

const int	STD_ERROR_MSG_LEN	= sizeof(STD_ERROR_MSG) - 1;

#define		STD_BYE_MSG		"Good bye!"

const int	STD_BYE_MSG_LEN		= sizeof(STD_BYE_MSG) - 1;

const int	ERROR_FD		= -1;


struct		InfoForClient
{
  int		fd_;
  int		threadNum_;
};

void* readFile(int clientFD, int fileNum)
{
    char fileName[BUFFER_LEN];
    char buffer[BUFFER_LEN];
    int fileFD = open(FILENAME,O_RDONLY,0440);
}


//---		Definition of global vars:				---//

//  PURPOSE:  To be non-zero for as long as this program should run, or '0'
//	otherwise.


//---		Definition of functions:				---//



//  PURPOSE:  To cast 'vPtr' to the pointer type coming from 'doServer()'
//	that points to two integers.  Then, to use those two integers,
//	one as a file descriptor, the other as a thread number, to fulfill
//	requests coming from the client over the file descriptor socket.
//	Returns 'NULL'.
void*		handleClient	(void*		vPtr
				)
{
  //  I.  Application validity check:
  if(vPtr==NULL){
    fprintf(stderr,"Null ptr to handleClient(),exiting\n");
    exit(EXIT_FAILURE);
  }

  //  II.  Handle client:


  struct    InfoForClient* infoPtr;
  infoPtr = (struct InfoForClient*)vPtr;

  int 	   fd		= infoPtr->fd_;	
  int	   threadNum	= infoPtr->threadNum_ ;	
  free(infoPtr);

  printf("Thread number %d starting\n" ,threadNum);

  //  II.B.  Read command:
  char	buffer[BUFFER_LEN];
  char	command;
  int 	shouldContinue	= 1;

  while  (shouldContinue)
  {
    read(fd,buffer,BUFFER_LEN);
    printf("Thread %d received: %s\n",threadNum,buffer);
    command	= buffer[0];

    if(command==WHOLE_FILE_CMD_CHAR)
    {
        int newFD = open(FILENAME,O_RDONLY);

        if(newFD<0)
        {
            fprintf(stderr,"Cannot open %s\n",FILENAME);
            exit(EXIT_FAILURE);
        }

        char buffer1[BUFFER_LEN];
        int readBuffer = read(newFD,buffer1,BUFFER_LEN);

        if(readBuffer<0)
        {
          fprintf(stderr,"Cannot open %s\n",FILENAME);
          exit(EXIT_FAILURE);
        }

        write(fd,buffer1,BUFFER_LEN);
        close(newFD);
    }

    if (command ==QUIT_CMD_CHAR)
        {
            write(fd,STD_BYE_MSG,STD_BYE_MSG_LEN);
            shouldContinue =0;
        }

    if (command=='1' || command == '2' || command== '3' || command =='4')
    {
        int pid;
        int columnIndex;

        pid=fork();
        if(pid<0)
        {
            fprintf(stderr,"%c\n",STD_ERROR_MSG[STD_ERROR_MSG_LEN]);
            exit(EXIT_FAILURE);
        }
        else if(pid ==0)
        {
            int newFD2 = open(FILENAME,O_RDONLY);
            if(newFD2<0)
            {
                fprintf(stderr,"%c\n",STD_ERROR_MSG[STD_ERROR_MSG_LEN]);
                exit(EXIT_FAILURE);
            }
            dup2(newFD2,STDIN_FILENO);
            dup2(fd,STDOUT_FILENO);


            char awkCmd[BUFFER_LEN];
            snprintf(awkCmd,BUFFER_LEN,"{print $%c}",columnIndex);
            execl("/usr/bin/awk","/usr/bin/awk",awkCmd,NULL);        

            close(newFD2);
        }

        else
        {
            wait(NULL);
        }  
    }
  }



  //  III.  Finished:
  printf("Thread %d quitting.\n",threadNum);
  return(NULL);
  
}


//  PURPOSE:  To run the server by 'accept()'-ing client requests from
//	'listenFd' and doing them.




void		doServer	(int		listenFd
				)
{
  //  I.  Application validity check:

  //  II.  Server clients:
  pthread_t		threadId;
  pthread_attr_t	threadAttr;
  int			threadCount	= 0;
  struct InfoForClient*	infoPtr ;

  infoPtr	= (struct InfoForClient*)malloc(sizeof(struct InfoForClient));
  
 pthread_attr_init(&threadAttr);
 pthread_attr_setdetachstate(&threadAttr,PTHREAD_CREATE_DETACHED);

  

  while  (1)
  {

    infoPtr	= (struct InfoForClient*)malloc(sizeof(struct InfoForClient));

    infoPtr->fd_	= accept(listenFd,NULL,NULL);
    infoPtr->threadNum_	= threadCount++;

    pthread_create(&threadId,&threadAttr,handleClient,infoPtr);
  }
    pthread_attr_destroy(&threadAttr);



  //  III.  Finished:
}


//  PURPOSE:  To decide a port number, either from the command line arguments
//	'argc' and 'argv[]', or by asking the user.  Returns port number.
int		getPortNum	(int	argc,
				 char*	argv[]
				)
{
  //  I.  Application validity check:

  //  II.  Get listening socket:
  int	portNum;

  if  (argc >= 2)
    portNum	= strtol(argv[1],NULL,0);
  else
  {
    char	buffer[BUFFER_LEN];

    printf("Port number to monopolize? ");
    fgets(buffer,BUFFER_LEN,stdin);
    portNum	= strtol(buffer,NULL,0);
  }

  //  III.  Finished:  
  return(portNum);
}


//  PURPOSE:  To attempt to create and return a file-descriptor for listening
//	to the OS telling this server when a client process has connect()-ed
//	to 'port'.  Returns that file-descriptor, or 'ERROR_FD' on failure.
int		getServerFileDescriptor
				(int		port
				)
{
  //  I.  Application validity check:

  //  II.  Attempt to get socket file descriptor and bind it to 'port':
  //  II.A.  Create a socket
  int socketDescriptor = socket(AF_INET, // AF_INET domain
			        SOCK_STREAM, // Reliable TCP
			        0);

  if  (socketDescriptor < 0)
  {
    perror("socket()");
    return(ERROR_FD);
  }

  //  II.B.  Attempt to bind 'socketDescriptor' to 'port':
  //  II.B.1.  We'll fill in this datastruct
  struct sockaddr_in socketInfo;

  //  II.B.2.  Fill socketInfo with 0's
  memset(&socketInfo,'\0',sizeof(socketInfo));

  //  II.B.3.  Use TCP/IP:
  socketInfo.sin_family = AF_INET;

  //  II.B.4.  Tell port in network endian with htons()
  socketInfo.sin_port = htons(port);

  //  II.B.5.  Allow machine to connect to this service
  socketInfo.sin_addr.s_addr = INADDR_ANY;

  //  II.B.6.  Try to bind socket with port and other specifications
  int status = bind(socketDescriptor, // from socket()
		    (struct sockaddr*)&socketInfo,
		    sizeof(socketInfo)
		   );

  if  (status < 0)
  {
    perror("bind()");
    return(ERROR_FD);
  }

  //  II.B.6.  Set OS queue length:
  listen(socketDescriptor,5);

  //  III.  Finished:
  return(socketDescriptor);
}


int		main		(int	argc,char*	argv[])
{
  //  I.  Application validity check:
  //  II.  Do server:
  int 	      port	= getPortNum(argc,argv);
  int	      listenFd	= getServerFileDescriptor(port);
  int	      status	= EXIT_FAILURE;

  if  (listenFd >= 0)
  {
    doServer(listenFd);
    close(listenFd);
    status	= EXIT_SUCCESS;
  }
  //  III.  Finished:
  return(status);
}
