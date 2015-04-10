node('slave') {
    git url: '/data/workflow-plugin-pipeline-demo/'
    def mvnHome = tool 'M3'
    env.PATH = "${mvnHome}/bin:${env.PATH}"
    stage 'Dev'
    sh 'mvn clean package'
    archive 'target/x.war'
    sh "mvn cargo:redeploy -Dhost=${hostDev}"
}

stage 'QA'
parallel(longerTests: {
    node('1') {
        sh "mvn -f sometests/pom.xml test -Durl=http://${hostDev}:8080/demo-war/ -Dduration=30"
    }
}, quickerTests: {
    node('2') {
        sh "mvn -f sometests/pom.xml test -Durl=http://${hostDev}:8080/demo-war/ -Dduration=20"
    }
})

checkpoint('Passed tests, ready to deploy to ${hostStage}')
stage name: 'Staging', concurrency: 1
node('slave') {
    sh "mvn cargo:redeploy -Dhost=${hostStage}"
}

input message: "Does http://${hostStage}:8080/demo-war/ look good?"
checkpoint('Before production deployment')
stage name: 'Production', concurrency: 1
node('slave') {
    unarchive mapping: ['target/x.war' : 'x.war']
    sh "mvn cargo:redeploy -Dhost=${hostProd}"
    echo 'Deployed to http://${hostProd}:8080/demo-war/'
}
