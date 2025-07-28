pipeline {
    agent any
    environment {
        REPO_URL = 'https://maven.minersonline.uk/snapshots'
        REPO_USER = credentials('maven-repo-username')
        REPO_PASSWORD = credentials('maven-repo-password')
    }
    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/miners-online/minestom-mod-compat', branch: 'main'
            }
        }
        stage('Set Executable Permissions') {
            steps {
                // Ensure gradlew has the right permissions
                sh 'chmod +x ./gradlew'
            }
        }
        stage('Build with Gradle') {
            steps {
                sh './gradlew clean build'
            }
        }
        stage('Publish Artifacts') {
            steps {
                sh "./gradlew publish"
            }
        }
    }
    post {
        success {
            echo 'Build and publish completed successfully.'
        }
        failure {
            echo 'Build or publish failed.'
        }
    }
}
