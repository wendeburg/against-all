#Deriving the latest base image
FROM python:latest


# Any working directory can be chosen as per choice like '/' or '/home' etc
# i have chosen /usr/app/src
WORKDIR /usr/app/src

#to COPY the remote file at working directory in container
COPY AA_Player.py ./
# Now the structure looks like this '/usr/app/src/AA_Player.py'
COPY requirements.txt requirements.txt
RUN pip install -r requirements.txt

#CMD instruction should be used to run the software
#contained by your image, along with any arguments.