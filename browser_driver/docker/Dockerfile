FROM ubuntu:16.10

RUN apt-get update

# UTILITIES

RUN apt-get install -y lsof ffmpeg git

# MITMPROXY 

RUN apt-get install -y python-pip python-dev libffi-dev libssl-dev libxml2-dev libxslt1-dev libjpeg8-dev zlib1g-dev
RUN pip install --upgrade pip
RUN pip install mitmproxy
RUN pip install virtualenv
RUN git clone https://github.com/christofferqa/mitmproxy && \
    cd mitmproxy && \
    ./dev.sh && \
    echo "Installed mitmproxy in $(pwd)"
RUN rm -rf ~/.cache/pip 

ENV MITMPROXY_DIR /mitmproxy


# Force certificates generation
#RUN mitmdump -s "echo s" || true
#RUN mkdir /usr/share/ca-certificates/extra
#RUN cp /root/.mitmproxy/mitmproxy-ca-cert.cer /usr/share/ca-certificates/extra/
#RUN update-ca-certificates


# GOOGLE CHROME and PROTRACTOR

WORKDIR /tmp
RUN apt-get install -y npm nodejs-legacy
RUN npm install -g http-server
RUN npm install -g protractor mocha jasmine && \
    webdriver-manager update && \
    apt-get install -y xvfb wget default-jre && \
    wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    dpkg --unpack google-chrome-stable_current_amd64.deb && \
    apt-get install -f -y && \
    apt-get clean && \
    rm google-chrome-stable_current_amd64.deb 


#
# JAVA and other TAJS dependencies
#

RUN apt-get install -y software-properties-common; \
    add-apt-repository -y ppa:webupd8team/java; \
    add-apt-repository -y ppa:cwchien/gradle; \
    apt-get update; \
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections; \
    apt-get install -y \
        ant \
        curl \
        git \
        gnuplot \
        netcat \
        gradle \
        oracle-java8-installer && \
    apt-get clean

# UTILITIES

RUN apt-get install -y tidy wget

# EXPOSE 8080 8081

ADD scripts/* /usr/local/bin/

# MITMPROXY 

RUN mkdir /out
RUN chmod ugo+rwx /out
VOLUME /out

#USER mitmproxy
#RUN mkdir /home/mitmproxy/.mitmproxy
#VOLUME /home/mitmproxy/.mitmproxy
WORKDIR /root
