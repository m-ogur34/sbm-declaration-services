// Build / test / deploy skeleton. The agent needs access to the internal Nexus, which
// proxies every dependency this project uses.
pipeline {
    agent { label 'maven-jdk21' }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['sc-test', 'sc-uat', 'prep', 'live', 'dr'],
               description: 'helm/values/<ENVIRONMENT>.yaml')
        booleanParam(name: 'DEPLOY', defaultValue: false, description: 'Deploy after a green build')
    }

    environment {
        MAVEN_OPTS = '-Dfile.encoding=UTF-8'
        IMAGE_NAME = 'registry.allianz.com.tr/ysv/sbm-declaration-services'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                // verify runs the tests, the JaCoCo report and the coverage gate
                // (LINE >= 0.90, BRANCH >= 0.85); the build fails below either threshold.
                sh './mvnw -B -ntp clean verify'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                    archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('Package Image') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} ."
                sh "docker push ${IMAGE_NAME}:${BUILD_NUMBER}"
            }
        }

        stage('Deploy') {
            when { expression { return params.DEPLOY } }
            steps {
                sh """
                    helm upgrade --install sbm-declaration-services helm/chart \
                        -f helm/values/${params.ENVIRONMENT}.yaml \
                        --set springboot-deployment.app.image.tag=${BUILD_NUMBER} \
                        --wait --timeout 5m
                """
            }
        }
    }

    post {
        failure {
            echo 'Build failed. Coverage gate: LINE >= %90, BRANCH >= %85.'
        }
    }
}
