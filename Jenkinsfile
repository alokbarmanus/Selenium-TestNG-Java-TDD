pipeline {
    agent any

    parameters {
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run the TestNG Selenium suite'
        )
        choice(
            name: 'TEST_ENV',
            choices: ['dev', 'sit', 'uat'],
            description: 'Target environment for the test suite'
        )
    }

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    environment {
        CI = 'true'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/alokbarmanus/Selenium-TestNG-Java-BDD.git'
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn -B -ntp -DskipTests compile test-compile'
            }
        }

        stage('Run Tests') {
            when {
                expression { return params.RUN_TESTS }
            }
            steps {
                sh "mvn -B -ntp test -Denv=${params.TEST_ENV} -Dheadless=true"
            }
            post {
                always {
                    // TestNG / Surefire XML results
                    junit testResults: 'target/surefire-reports/**/*.xml',
                          allowEmptyResults: true

                    script {
                        if (fileExists('target/cucumber-reports/index.html')) {
                            publishHTML(target: [
                                reportName            : 'Cucumber Report',
                                reportDir             : 'target/cucumber-reports',
                                reportFiles           : 'index.html',
                                keepAll               : true,
                                alwaysLinkToLastBuild : true,
                                allowMissing          : true
                            ])
                        } else {
                            echo 'Cucumber report not found at target/cucumber-reports/index.html. Skipping publish.'
                        }

                        if (fileExists('target/extent-reports/TestExecutionReport.html')) {
                            publishHTML(target: [
                                reportName            : 'Extent Report',
                                reportDir             : 'target/extent-reports',
                                reportFiles           : 'TestExecutionReport.html',
                                keepAll               : true,
                                alwaysLinkToLastBuild : true,
                                allowMissing          : true
                            ])
                        } else {
                            echo 'Extent report not found at target/extent-reports/TestExecutionReport.html. Skipping publish.'
                        }

                        String testngReportFile = "${params.TEST_ENV.toUpperCase()}_Execution.html"
                        if (fileExists("target/surefire-reports/Selenium-TestNG-Java-BDD/${testngReportFile}")) {
                            publishHTML(target: [
                                reportName            : 'TestNG Report',
                                reportDir             : 'target/surefire-reports/Selenium-TestNG-Java-BDD',
                                reportFiles           : testngReportFile,
                                keepAll               : true,
                                alwaysLinkToLastBuild : true,
                                allowMissing          : true
                            ])
                        } else {
                            echo "TestNG report not found at target/surefire-reports/Selenium-TestNG-Java-BDD/${testngReportFile}. Skipping publish."
                        }
                    }

                    // Archive all report artifacts
                    archiveArtifacts artifacts: '''
                        target/surefire-reports/**,
                        target/extent-reports/**,
                        target/cucumber-reports/**,
                        test-output/**
                    ''', allowEmptyArchive: true
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully for environment: ${params.TEST_ENV}"
        }
        failure {
            echo "Pipeline failed for environment: ${params.TEST_ENV}"
        }
        always {
            cleanWs()
        }
    }
}
