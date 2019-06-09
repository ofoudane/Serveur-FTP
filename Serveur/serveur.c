#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <pthread.h>
#include <dirent.h>
#include <semaphore.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/stat.h>

#define STRING_SIZE 100
#define TOKEN_SIZE  12
#define USERNAME_MIN_LENGTH 2
#define PASSWORD_MIN_LENGTH 8

#define PUBLIC_PATH		"#~public"


#define FAILED 					0
#define SUCCESS 				1
#define NOT_FOUND 			-1

#define WRONG_PASSWORD 	0
#define UNKNOWN_USER		-1

#define USER_ALREADY_EXISTS 0
#define INVALID_CREDENTIALS -1

#define FILE_DETAILS_COMMAND 9
#define DISCONNECT_COMMAND   8
#define CREATE_DIR_COMMAND	 7
#define PUBLIC_STATE_COMMAND 	  6
#define DELETE_COMMAND  5
#define REGISTER_COMMAND 4
#define DISPLAY_COMMAND 3
#define UPLOAD_COMMAND  2 
#define DOWNLOAD_COMMAND 1
#define CONNECTION_COMMAND 0

#define END_COMMAND -1

typedef struct {
  char data[STRING_SIZE];
//  char authToken[TOKEN_SIZE];
  int code;
} query;




typedef struct {
  char username[STRING_SIZE];
  char password[STRING_SIZE];
} user;


static char ** publicFiles 				= NULL;
static user ** userList    				= NULL;
static sem_t * userReadSemaphore   		= NULL;
static sem_t * publicReadSemaphore 		= NULL;


int createServer(struct sockaddr * sa){
  //Initialisé à -1 et ne changera qu'à la fin de la création complète de la socket
  int result = -1;
  
  int serverSocket = socket(PF_INET, SOCK_STREAM, 0);

  if(serverSocket != -1){
    int err = bind(serverSocket, sa, sizeof(struct sockaddr));
    if(err != -1){
      //Le deuxième paramètre est le nombre maximal des connexions maintenus en même temps
      err = listen(serverSocket, SOMAXCONN);
      if(err != -1){//Création de la socket réussi
        result = serverSocket;
      } 
    }
  }

  return result;
}



int createConnection(int serverSocket, struct sockaddr * sa){
  printf("waiting for connection...\n");
  fflush(stdout);
  int size_sa = sizeof(struct sockaddr);
  return accept(serverSocket, sa, &size_sa);  
}



/*******************************************************************/
/********************* START - QUERY UTILS *************************/
/*******************************************************************/

query * createQuery(char data[STRING_SIZE],  int code){
  query * result = (query*) calloc(1, sizeof(query));
  
  if(result != NULL){
    memcpy(result->data, data, sizeof(char) * STRING_SIZE);
    result->code = code;
  }

  return result;
}



user * createUser(char * username, char * password){
	user * newUser = (user*) calloc(1, sizeof(user));

	if(newUser != NULL){
		strcpy(newUser->username, username);
		strcpy(newUser->password, password);
	}

	return newUser;
}


int sendQuery(int socket, query* aQuery){
  int writeSize = 0;

  if(socket != -1){
    writeSize = send(socket, aQuery, sizeof(query), 0);
  }

  return writeSize;
}

query* readQuery(int socket){
  if(socket == -1){
    return NULL;
  }else{
    query * clientQuery = (query*) calloc(1, sizeof(query));
    int received = recv(socket, clientQuery, sizeof(query), MSG_WAITALL);

    if(received != sizeof(query)){
    	free(clientQuery);
    	clientQuery = NULL;
    }

    return clientQuery;
  }
}

/*******************************************************************/
/*********************** END - QUERY UTILS *************************/
/*******************************************************************/


/*******************************************************************/
/********************* START - SEMAPHORE ***************************/
/*******************************************************************/

void initSemaphores(){
	userReadSemaphore   	= (sem_t*) malloc(sizeof(sem_t)) ;
	publicReadSemaphore 	= (sem_t*) malloc(sizeof(sem_t)) ;

	sem_init(userReadSemaphore, 0, 1);
	sem_init(publicReadSemaphore, 0, 1);

}


void destroySemaphores(){
	if(userReadSemaphore != NULL) free(userReadSemaphore);
	if(publicReadSemaphore != NULL) free(publicReadSemaphore);

}

/*******************************************************************/
/*********************** END - SEMAPHORE ***************************/
/*******************************************************************/


char** getDirFiles(char * name){
  char ** filenameList = (char**) malloc(sizeof(char*));

  int  counter = 1;

  DIR * dir = opendir(name);

  struct dirent * ent;
  
  while(ent = readdir(dir)){
    counter++;
    
    filenameList = (char**) realloc(filenameList, sizeof(char*) * counter);
    
    filenameList[counter - 2] = (char*) malloc(sizeof(char) * (strlen(ent->d_name) + 1));

    strcpy(filenameList[counter - 2], ent->d_name);
  }

  filenameList[counter - 1] = NULL;

  closedir(dir);

  return filenameList;
}


void sendStopQuery(int socket){
  query * stopQuery = createQuery("", END_COMMAND);

  sendQuery(socket, stopQuery);

  if(stopQuery) free(stopQuery);
}



void removeEndLine(char * source){
  int size = strlen(source);
  
  if(source[size - 1] == '\n'){
    source[size - 1] = '\0';
  }

}

char * escapePath(char * path){
	int size = strlen(path);
	char * result = (char*) malloc(sizeof(char));
	int index = 0;
	int counter = 1;

		
	while(index < size && (path[index] == '/' || path[index] == '~' || path[index] == '.')){
		index++;
	} 
		
	while(index < size){
		if((index + 2) < size && path[index] == '/' && path[index + 1] == '.' && path[index + 2] == '.') {
				index += 2;
		}else{
			counter++;
			result = (char*) realloc(result, sizeof(char) * counter);
			result[counter - 2] = path[index];
		}

		index++;
	}

	result[counter - 1] = '\0';

	return result;
}




int fileExists(char * path){
	
	FILE * fp = fopen(path, "r");
	int exists = 0;

	if(fp != NULL){
		fclose(fp);
		exists = 1;
	}

	return exists;
}

char * readLine(FILE* filePointer, int maxSize){
	char * ligne = (char*) malloc(sizeof(char));
	char carac = ' ';
	int nbChars = 1;

	while(carac != '\n' && carac != EOF && (nbChars - 1) < maxSize){
		carac = fgetc(filePointer);
		if(carac != '\n' && carac != EOF){
			nbChars++;
			ligne = (char*) realloc(ligne, sizeof(char) * nbChars);
			ligne[nbChars - 2] = carac;
		}
	}

	ligne[nbChars - 1] = '\0';

	return ligne;
}


void freeStrList(char ** list){
	int index = 0;
	
	while(list[index] != NULL){
		free(list[index]);
		index++;
	}

	free(list);
}



/*******************************************************************/
/********************* START - USER HANDLER ************************/
/*******************************************************************/
int userSize(user ** userList){  
  int size = 0;

  while(*(userList + size) != NULL){
    size++;
  }

  return size + 1;//Add null
}

user* findUser(char * username){
	user* 	foundUser 	= NULL;
	int 	index 		= 0;

	sem_wait(userReadSemaphore);

	while(userList[index] != NULL && foundUser == NULL){
		if(strcmp(userList[index]->username, username) == 0){
			foundUser = userList[index];
		}
		index++;
	}


	sem_post(userReadSemaphore);

	return foundUser;
}


int validateUser(user * userToValidate){
  int isValid = 0;

  if(userToValidate != NULL) {
    if(strlen(userToValidate->username) >= USERNAME_MIN_LENGTH && strlen(userToValidate->password) >= PASSWORD_MIN_LENGTH)
      isValid = 1;
  }

  return isValid;
}


user ** loadUserList(){
  FILE * fp = fopen("userList.o", "r");
    

  if(userList != NULL){
  	free(userList);
  }

	sem_wait(userReadSemaphore);

  userList = (user**) malloc(sizeof(user*));

  int counter = 1;

  if(fp != NULL){
    user * tempContent = (user*) calloc(1, sizeof(user)); 

    int readCount = fread(tempContent, sizeof(user), 1, fp);

    while(feof(fp) == 0 && readCount == 1){
      counter++;
      
      userList = (user**) realloc(userList, sizeof(user*) * counter);
      
      userList[counter - 2] = tempContent;
      
      tempContent = (user*) calloc(1, sizeof(user));

      readCount = fread(tempContent, sizeof(user), 1, fp);
    }

    fclose(fp);

    free(tempContent);
  }


  userList[counter - 1] = NULL;

	sem_post(userReadSemaphore);

  return userList;
}

void saveUsers(user ** newUserList){
  FILE * fp = fopen("userList.o", "w");
  int index = 0;

  if(fp != NULL){

  	  while(newUserList[index] != NULL){
		    fwrite(*(newUserList + index), sizeof(user), 1, fp);
		    index++;
		  }

		  sem_wait(userReadSemaphore);

		  if(userList != NULL && userList != newUserList) free(userList);
		  userList = newUserList;
			
			sem_post(userReadSemaphore);

	    fclose(fp);
		  
  }
}

int addUser(user * userToAdd){
	int isAdded = INVALID_CREDENTIALS;

  if(validateUser(userToAdd) == 1 && findUser(userToAdd->username) == NULL){
    sem_wait(userReadSemaphore);

    int size = userSize(userList);
    
    userList = (user**) realloc(userList, sizeof(user) * ( size + 1));

    userList[size - 1] = (user*) malloc(sizeof(user));

    memcpy(userList[size - 1], userToAdd, sizeof(user));

    userList[size] = NULL;

    mkdir(userToAdd->username, 0700);

    sem_post(userReadSemaphore);

    saveUsers(userList);

    isAdded = SUCCESS;
  }else if(validateUser(userToAdd) == 1){
  	isAdded = USER_ALREADY_EXISTS;
  }

  return isAdded;
}


/*
  * @return -1 : If the user doesn't exists.
  * @return 0  : If the password is not valid.
  * @return 1  : If the password was changed succesfuly.
*/
int updatePassword(char * password, char * username){
	user * foundUser = findUser(username);
	int    updated   = -1;

	if(foundUser != NULL && strlen(password) >= PASSWORD_MIN_LENGTH){
		strcpy(foundUser->password, password);
		updated 			= 1;
		saveUsers(userList);
	}else if(foundUser != NULL){
		updated 			= UNKNOWN_USER;
	}

	return updated;
}

/*
  * @return -1 : If the user doesn't exists.
  * @return 0  : If the password is wrong.
  * @return 1  : If the user exists and password is correct.
*/
int isAuthenticated(char * username, char * password){
  int isAuth = UNKNOWN_USER;
  int index  = 0 ; 
	
	sem_wait(userReadSemaphore);
  
  while(userList[index] != NULL && isAuth == -1){
    
    if(strcmp(userList[index]->username, username) == 0){
      if(strcmp(userList[index]->password, password) == 0){
        isAuth = SUCCESS;
      }else{
        isAuth = WRONG_PASSWORD;
      }
    }

    index++;
  }
  sem_post(userReadSemaphore);

  return isAuth;

}


/*******************************************************************/
/************************ end - AUTH HANDLER ***********************/
/*******************************************************************/


/*******************************************************************/
/********************* START - PUBLIC FILES ***********************/
/*******************************************************************/
int stringListSize(char ** strList){
  int size = 0;

  while(*(strList + size) != NULL){
    size++;
  }

  return size + 1;
}



char ** loadPublicFiles(){

	int counter = 1;
	FILE * fp = fopen("./public.txt", "r");
	char * filePath;
	
	if(publicFiles != NULL) free(publicFiles);

	publicFiles = (char**) malloc(sizeof(char*));

	if(fp != NULL){

		while(feof(fp) == 0) {
			filePath = readLine(fp, STRING_SIZE);
			
			if(fileExists(filePath) == 0){
				free(filePath);
			}else{
				counter++;
				
				publicFiles = (char**) realloc(publicFiles, sizeof(char*) * counter);
				
				publicFiles[counter - 2] = filePath;
			}

		}


		fclose(fp);
	}

	publicFiles[counter - 1] = NULL;

	return publicFiles;
}


int isPublicFile(char * path){
	int index = 0;
	int isPublic = 0;

	if(fileExists(path) == 1){
		while(publicFiles[index] != NULL && isPublic == 0){
			if(strcmp(publicFiles[index], path) == 0){
				isPublic = 1;
			}
			index++;
		}
	}
	return isPublic;
}

void savePublicFiles(char ** newPublicFiles){
	int index = 0;
	FILE * fp = fopen("./public.txt", "w");

	if(fp != NULL){
		while(newPublicFiles[index] != NULL){
			fputs(newPublicFiles[index], fp);
			fputc('\n', fp);
			index++;
		}
		
		if(publicFiles != NULL && publicFiles != newPublicFiles) free(publicFiles);

		publicFiles = newPublicFiles;

		fclose(fp);
	}

}


char ** addPublicFiles(char * file){
	
	if(isPublicFile(file) == 0){
		int size = stringListSize(publicFiles);
		
		publicFiles = (char**) realloc(publicFiles, sizeof(char*) * (size + 1));

		publicFiles[size - 1] = (char*) malloc(sizeof(char) * (strlen(file) + 1));

		strcpy(publicFiles[size - 1], file); 

		publicFiles[size] = NULL;

		savePublicFiles(publicFiles);
	}

	return publicFiles;
}


int  removeFromPublicFiles(char * fileToRemove) {
	if(publicFiles != NULL && isPublicFile(fileToRemove) == 1){
		int listSize =  stringListSize(publicFiles);
		char ** newList = (char**) malloc(sizeof(char*));
		int counter = 1;

		for(int i = 0; i < listSize - 1; i++){
			if(strcmp(publicFiles[i], fileToRemove) != 0){
				counter++;
				
				newList = (char**) realloc(newList, sizeof(char*) * counter);
				
				newList[counter - 2] = publicFiles[i];
			}else{
				free(publicFiles[i]);
			}
		}

		newList[counter - 1] = NULL;

		if(publicFiles != NULL) free(publicFiles);

		publicFiles = newList;

		savePublicFiles(newList);

		return 1;
	}else{
		return 0;
	}

}





/*******************************************************************/
/********************* END - PUBLIC FILES ***********************/
/*******************************************************************/



/*******************************************************************/
/********************* START - EVENT HANDLER ***********************/
/*******************************************************************/

char * mergeDirAndPath(char * dir, char * filename){
	char * fullPath = (char*) calloc(1, sizeof(char) * (strlen(dir) + strlen(filename) + 1));

	strcpy(fullPath, dir);
	strcat(fullPath, filename);

	return fullPath;
}

char * buildFilePath(char * username, char * directory){
	char * escapedDir = escapePath(directory);
	char * correctPath = (char*) calloc(1, sizeof(char) * (strlen(username) + strlen(escapedDir) + 2));


	strcpy(correctPath, username);
	strcat(correctPath, "/");
	strcat(correctPath, escapedDir);

	free(escapedDir);

	return correctPath;
}


void handleDisplayCommand(char * username, int socket, char * directoryToRead){

  char ** files;

  char *  relativePath;

  char * directory = NULL;

  int index = 0;

  query response;


	if(strcmp(directoryToRead, PUBLIC_PATH) == 0 || username == NULL){
		files = publicFiles;
	}else{
		directory = buildFilePath(username, directoryToRead);

		files = getDirFiles(directory);
	}


  while(files[index] != NULL){
  	memset(response.data, 0, sizeof(char) * STRING_SIZE);

    strcpy(response.data, files[index]);
    
    if(directory != NULL){
	    relativePath = mergeDirAndPath(directory, files[index]);
	    response.code = isPublicFile(relativePath);
	    free(relativePath);  	
    }else if(files == publicFiles){
    	response.code = 1;
    }

    sendQuery(socket, &response);
    
    index++;
  }

  sendStopQuery(socket);

  if(files != NULL && files != publicFiles) freeStrList(files);
  if(directory != NULL) free(directory);
}


void handleDownloadCommand(char * username, int socket, char * path){
  char * correctFilePath = NULL; 

	if(isPublicFile(path)){
		correctFilePath = escapePath(path);
	}else if(username != NULL){
		correctFilePath = buildFilePath(username, path);
	}

	FILE * fp = NULL;
	
	if(correctFilePath != NULL){
		  fp = fopen(correctFilePath, "r");
	}
  
  char content[STRING_SIZE];
  
  int readSize = STRING_SIZE;

  query * queryToSend = NULL;

  if(fp != NULL){

    printf("Sending file : %s\n", correctFilePath);

    while(readSize == STRING_SIZE){
      memset(content, 0, sizeof(char) * STRING_SIZE);

      readSize = fread(content, sizeof(char), STRING_SIZE, fp);
      
      queryToSend = createQuery(content, readSize);

      sendQuery(socket, queryToSend);

      if(queryToSend != NULL) free(queryToSend);
    }

    fclose(fp);
  }else{
    queryToSend = createQuery("Inéxistant", NOT_FOUND);
    
    sendQuery(socket, queryToSend);

    if(queryToSend != NULL) free(queryToSend);
  }

  sendStopQuery(socket);

  if(correctFilePath != NULL){
  	free(correctFilePath);
  }

}



void handleUploadCommand(char * username, int socket, char * destination){
  
	FILE * fp = NULL;

  query * response;

  if(username != NULL){
		char * correctFilePath = buildFilePath(username, destination);

	  fp = fopen(correctFilePath, "w");
	
	  if(correctFilePath != NULL) free(correctFilePath);
	}

  query * clientQuery = readQuery(socket);
  
  int success = 1;

  while(clientQuery != NULL && clientQuery->code != END_COMMAND){
  
    if(fp != NULL){
      fwrite(clientQuery->data, sizeof(char), clientQuery->code, fp);
    }else{
      success = 0;
    }   
    
    free(clientQuery);

    clientQuery = readQuery(socket);

  };

  if(fp != NULL){
  	response = createQuery("File upload succesfuly", SUCCESS);
    fclose(fp);
  }else{
  	response = createQuery("File upload Failed", FAILED);
  }

  sendQuery(socket, response);

  if(response) free(response);

}


int handleConnectionCommand(int socket, char * username){
	query * passwordQuery = readQuery(socket);

	int isAuthorized = isAuthenticated(username, passwordQuery->data);
	
	query * response;

	if(isAuthorized == SUCCESS){
		response = createQuery("Connected", SUCCESS);
	}else if(isAuthorized == WRONG_PASSWORD){
		response = createQuery("Wrong password", WRONG_PASSWORD);
	}else{
		response = createQuery("Unknown user", UNKNOWN_USER);
	}

	sendQuery(socket, response);

	if(passwordQuery != NULL)	free(passwordQuery);
	if(response != NULL) free(response);

	return isAuthorized;
}


void handleRegisterCommand(int socket, char * username){
	query * passwordQuery = readQuery(socket);

	user * userToAdd = createUser(username, passwordQuery->data);

	int isAdded = addUser(userToAdd);

	query * response ;

	if(isAdded == SUCCESS){
		response = createQuery("User succesfuly registered", SUCCESS);
	}else if(isAdded == USER_ALREADY_EXISTS){
		response = createQuery("You cannot use this username as it already exists", USER_ALREADY_EXISTS);
	}else {
		response = createQuery("Your password must have at least 8 caracters and your username 2 caracters", INVALID_CREDENTIALS);
	}

	sendQuery(socket, response);

	free(response);
	free(passwordQuery);
	free(userToAdd);
}

void handleDeleteCommand(int socket, char * username, char * path){
	int deleted = FAILED;
	query * response ;

	if(username != NULL ){
		char * correctPath = buildFilePath(username, path);

		if(fileExists(correctPath) == 1){
			remove(correctPath);
			removeFromPublicFiles(correctPath);
			deleted = SUCCESS;
		}

		if(correctPath != NULL) free(correctPath);
	}

	if(deleted == SUCCESS){
		response = createQuery("File removed succesfuly", SUCCESS);
	}else{
		response = createQuery("Couldn't remove file", FAILED);
	}

	sendQuery(socket, response);

	if(response != NULL) free(response);
}


void handlePublicStateUpdate(int socket, char * username, char * path){
	int stateChanged = FAILED;

	query * clientQuery = readQuery(socket);

	query * response;

	if(username != NULL){
			char * correctPath = buildFilePath(username, path);

			if(fileExists(correctPath) == 1){

				if(clientQuery->code == 1){
					addPublicFiles(correctPath);
					stateChanged = SUCCESS;
				}
				else if(clientQuery->code == 0){
					removeFromPublicFiles(correctPath);
					stateChanged = SUCCESS;
				}
			}

			if(correctPath != NULL) free(correctPath);
	}


	if(stateChanged == SUCCESS){
		response = createQuery("State changed succesfuly", SUCCESS);
	}else{
		response = createQuery("State change failed : File doesn't exist", FAILED);
	}

	sendQuery(socket, response);

	if(clientQuery != NULL) free(clientQuery);
	if(response    != NULL) free(response);
}

void handleFileDetailsCommand(int socket, char * username, char * path){
	
	char * correctPath = NULL;

	query * response	 = NULL;

	if(username == NULL) {
		correctPath = escapePath(path);
	}else{
		correctPath = buildFilePath(username, path);
	}


	if(fileExists(correctPath) == 1 && (username != NULL || isPublicFile(correctPath))){
		struct stat fileStat;

		int error = stat(correctPath, &fileStat);

		if(error == -1){
			response = createQuery("Error calling the stat function", NOT_FOUND);
		}else{
			char * isFile;
			int    fileSize = (int) (fileStat.st_size / 1000);

			if(S_ISREG(fileStat.st_mode)){
				isFile = "FILE";
			}else{
				isFile = "DIRECTORY";
			}

			response = createQuery(isFile, fileSize);

		}
	}else{
		response = createQuery("File doesn't exists", NOT_FOUND);
	}

	sendQuery(socket, response);


	free(correctPath);
	free(response);

}


void handleCreateDir(int socket, char * username, char * dirPath){
	query * response;
	int result = -1;
	
	if(username != NULL){
		char * correctPath = buildFilePath(username, dirPath);
	
		result = mkdir(correctPath, 0700);

		if(correctPath != NULL) free(correctPath);
	}

	if(result == 0){	
		response = createQuery("Directory created succesfuly", SUCCESS);
	}else{
		response = createQuery("Directory creation failed", FAILED);
	}

	sendQuery(socket, response);

	if(response != NULL) free(response);
}
/*******************************************************************/
/*********************** END - QUERY UTILS *************************/
/*******************************************************************/


void * threadFunction(void * args) {
  int socket = *((int *) args);
  
  printf("User Socket connected ...\n");

  char * connectedUsername = NULL;

  query * clientQuery = readQuery(socket);

  while(clientQuery != NULL){

	  switch(clientQuery->code){
	    
	    case DISPLAY_COMMAND:
	      handleDisplayCommand(connectedUsername, socket, clientQuery->data);
	    break;
	    
	    case UPLOAD_COMMAND:
	      handleUploadCommand(connectedUsername, socket, clientQuery->data);
	    break;
	    
	    case DOWNLOAD_COMMAND:
	      handleDownloadCommand(connectedUsername, socket, clientQuery->data);
	    break;

	    case REGISTER_COMMAND:
	    	handleRegisterCommand(socket, clientQuery->data);
	    break;

	    case CONNECTION_COMMAND:
	    	if(handleConnectionCommand(socket, clientQuery->data) == SUCCESS){
	    		connectedUsername = (char*) malloc(sizeof(char) * (strlen(clientQuery->data) + 1));
	    		strcpy(connectedUsername, clientQuery->data);
	    	}
	    break;

	    case DELETE_COMMAND:
	    	handleDeleteCommand(socket, connectedUsername, clientQuery->data);
	    break;

	    case PUBLIC_STATE_COMMAND:
	    	handlePublicStateUpdate(socket, connectedUsername, clientQuery->data);
	    break;

	    case FILE_DETAILS_COMMAND:
	    	handleFileDetailsCommand(socket, connectedUsername, clientQuery->data);
	    break;

	    case DISCONNECT_COMMAND:
	    	free(connectedUsername);
	    	connectedUsername =  NULL;
	    break;

	    case CREATE_DIR_COMMAND:
	    	handleCreateDir(socket, connectedUsername, clientQuery->data);
	    break;

	    default:
	      printf("NO ACTION\n");
	    break;

	    free(clientQuery);
	    query * clientQuery = readQuery(socket);
	 	}


	  if(clientQuery != NULL) free(clientQuery);
		
		clientQuery = readQuery(socket);
  }

  
  pthread_exit(0);
}




void mainMultithreadServer(int port){
  struct sockaddr_in socketDetails;
  socketDetails.sin_family = AF_INET;
  socketDetails.sin_port   = htons(port);
  socketDetails.sin_addr.s_addr = INADDR_ANY;
  
  int serverSocket = createServer((struct sockaddr *) &socketDetails);

  if(serverSocket != -1){
    
    initSemaphores();
    loadPublicFiles();
    loadUserList();



    int clientSocket = createConnection(serverSocket, (struct sockaddr *) &socketDetails);
    
    int counter = 0;

    pthread_t * threadList = (pthread_t*) malloc(sizeof(pthread_t));


    while(1){
      counter++;
      
      threadList = (pthread_t*) realloc(threadList, sizeof(pthread_t) * counter);

      pthread_create(threadList + counter - 1, NULL, threadFunction, &clientSocket);  

      clientSocket = createConnection(serverSocket, (struct sockaddr *) &socketDetails);      
    }

    for(int j = 0; j < counter; j++){
      pthread_join(*(threadList + j), NULL);
    }

    free(threadList);
  }else{
    printf("Server socket creation failed\n");
  }

}



void testSaveList(){
  
  user ** list = (user**) malloc(sizeof(user**) * 3);
  
  list[0]      = (user*) calloc(1, sizeof(user));
  list[1]      = (user*) calloc(1, sizeof(user));
  list[2]      = NULL;

  strcpy(list[0]->username, "firstOmar");
  strcpy(list[0]->password, "Qmati@c!");

  strcpy(list[1]->username, "SecondOmar");
  strcpy(list[1]->password, "Qmati@c!2");


  saveUsers(list);
}


void testAddUser(){
  user ** list      = loadUserList();
  user * userToAdd  = (user*) calloc(1, sizeof(user));

  strcpy(userToAdd->username, "addOmar");
  strcpy(userToAdd->password, "Qmati@c!Add2");

  printf("IsAdded : %d", addUser(userToAdd));
  printf("IsAdded : %d", addUser(userToAdd));

}

void testCheckUser(){
	user ** list         = loadUserList();

	user * notFoundUser  = (user*) malloc(sizeof(user));
	user * wrongPassword = (user*) malloc(sizeof(user));
	user * correctAuth   = (user*) malloc(sizeof(user));

	strcpy(notFoundUser->username, "non-exists");
	strcpy(notFoundUser->password, "Qmati@c!Add");

	strcpy(wrongPassword->username, "addOmar");
	strcpy(wrongPassword->password, "wrong");

	strcpy(correctAuth->username, "addOmar");
	strcpy(correctAuth->password, "Qmati@c!Add2");

	printf("The not found user is : %d\n", isAuthenticated(notFoundUser->username, notFoundUser->password));
	printf("The wrong pass is : %d\n", isAuthenticated(wrongPassword->username, wrongPassword->password));
	printf("The correct user is : %d\n", isAuthenticated(correctAuth->username, correctAuth->password));
}



void test(){
	// testCheckUser();
	// testAddUser();
	printf("Escaped Path (./omar) = %s\n", escapePath("./omar"));
	printf("Escaped Path (//omar) = %s\n", escapePath("//omar"));
	printf("Escaped Path (~../../omar../../test) = %s\n", escapePath("../../omar../../test"));

}




int main(int argc, char *argv[]){
  if(argc == 1){
    test();
  }
  else if(argc == 2){
    mainMultithreadServer(atoi(argv[1]));
    exit(EXIT_SUCCESS);
  }else{
    printf("Usage : ./serveur [Port]\n");
    exit(EXIT_FAILURE);
  }
}


