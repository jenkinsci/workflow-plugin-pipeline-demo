node('slave') {
    git url: '/data/workflow-plugin-pipeline-demo/'
    def mvnHome = tool 'M3'
    env.PATH = "${mvnHome}/bin:${env.PATH}"
    stage 'Dev'
    sh 'mvn clean package'
    archive 'target/x.war'
    sh "mvn cargo:deploy -Dhost=${hostDev}"

    stage 'QA'

    parallel(longerTests: {
        sh "mvn -f sometests/pom.xml test -Durl=http://${hostDev}:8080/ -Dduration=30"
    }, quickerTests: {
        sh "mvn -f sometests/pom.xml test -Durl=http://${hostDev}:8080/ -Dduration=20"
    })
    stage name: 'Staging', concurrency: 1
    sh "mvn cargo:deploy -Dhost=${hostStage}"
}

input message: "Does ${hostStage} look good?"
try {
    checkpoint('Before production')
} catch (NoSuchMethodError _) {
    echo 'Checkpoint feature available in Jenkins Enterprise by CloudBees.'
}
stage name: 'Production', concurrency: 1
node('slave') {
    unarchive mapping: ['target/x.war' : 'x.war']
    sh "mvn cargo:deploy -Dhost=${hostProd}"
    echo 'Deployed to ${hostProd}'
}
