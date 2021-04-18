#!/bin/bash -xe

export PATH=${HOME}/.local/bin:${PATH}

EXPECTED_INSTAL_LOCATION=/opt/hostedtoolcache/jdk/11.0.10/x64
if [ -e "${EXPECTED_INSTAL_LOCATION}" ];
then
  for i in /opt/hostedtoolcache/jdk/11.0.10/x64/bin/*;
  do
    sudo update-alternatives --force --install /usr/bin/$(basename $i) $(basename $i) ${EXPECTED_INSTAL_LOCATION} 1
  done
fi

sudo -E apt-get install -y \
  python3 \
  python3-pip \
  python3-wheel \
  python3-distutils
pip3 install -U pip --upgrade --user
pip3 install setuptools>=50.1 --upgrade --user
pip3 install awsebcli awscli --upgrade --user

"${JAVA_HOME}"/bin/java -version

which javac
which java
which jar
which aws
which eb
