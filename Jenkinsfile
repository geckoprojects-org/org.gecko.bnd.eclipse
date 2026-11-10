pipeline  {
    agent any

    tools {
        jdk 'OpenJDK21'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {
        stage('Main branch release') {
            when { 
                branch 'main' 
            }
            steps {
                echo "I am building on ${env.BRANCH_NAME}"
                withCredentials([usernamePassword(credentialsId: 'dim-nexus', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                    sh "./gradlew clean build release -Drelease.dir=${WORKSPACE}/cnf/release --info --stacktrace -Dmaven.repo.local=${WORKSPACE}/.m2"
                }
            }
        }
        stage('Snapshot branch release') {
            when { 
                branch 'snapshot'
            }
            steps  {
                echo "I am building on ${env.JOB_NAME}"
                withCredentials([usernamePassword(credentialsId: 'dim-nexus', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                    sh "./gradlew release --info --stacktrace -Dmaven.repo.local=${WORKSPACE}/.m2"
                }
            }
        }
    }

}
