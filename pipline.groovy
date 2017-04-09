﻿// Jenkins Job pipeline groovy script.
// Autor: Sergey Shimansiy flyer8@yandex.ru
node {
    // Get Artifactory server instance, defined in the Artifactory Plugin administration page.
    def server = Artifactory.server "Artifactory_localhost"
    // Create an Artifactory Maven instance.
    def rtMaven = Artifactory.newMavenBuild()

    stage 'Clone sources'
    git url: 'https://github.com/mzagar/messaging-example.git'

    stage 'Artifactory configuration'
    // Tool name from Jenkins configuration
    rtMaven.tool = "Maven-3.3.9"
    // Set Artifactory repositories for dependencies resolution and artifacts deployment.
    rtMaven.deployer releaseRepo:'libs-release-local', snapshotRepo:'libs-snapshot-local', server: server
    rtMaven.resolver releaseRepo:'libs-release', snapshotRepo:'libs-snapshot', server: server

    stage 'Maven build'
    def buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean install'

    stage 'Publish build info'
    server.publishBuildInfo buildInfo
	
	stage 'Copy to Staging env'
	sh 'sudo -u root mkdir /opt/infobip-message/ || true'
	sh 'sudo -u root rm -rf /opt/infobip-message/*'
	sh 'sudo -u root rm -rf /opt/infobip-message/.* || true'
	sh 'sudo -u root git clone https://github.com/flyer8/infobip-message.git /opt/infobip-message/'
    sh 'sudo -u root cp -rf message-gateway/ /opt/infobip-message/'
	sh 'sudo -u root cp -rf message-processor/ /opt/infobip-message/'
		
	stage 'Deploy to Docker'
    // Need add Dockerfile and script from git
    sh 'sudo -u root docker rm -f -v message || true'
	sh 'sudo -u root docker rmi infobip/message || true'
	sh 'sudo -u root docker build --rm -t infobip/message /opt/infobip-message'
	sh 'sudo -u root docker run --name message -p 8888:8080 -d infobip/message'
	sh 'sudo -u root docker exec -d message ./start_app.sh'
}