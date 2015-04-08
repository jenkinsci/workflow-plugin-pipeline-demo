node('slave') {
    git url: 'https://github.com/kmadel/workflow-plugin-pipeline-demo.git'
    env.PATH="${tool 'Maven 3.x'}/bin:${env.PATH}"
    stage 'Dev'
    sh 'mvn -o clean package'
    archive 'target/x.war'

    stage 'QA'

    parallel(longerTests: {
        sh "mvn -o -f sometests/pom.xml test -Durl=${devHost} -Dduration=30"
    }, quickerTests: {
        sh "mvn -o -f sometests/pom.xml test -Durl=${devHost} -Dduration=20"
    })
    stage name: 'Staging', concurrency: 1
    sh "mvn cargo:redeploy -Dhost=${stageHost}"
}

input message: "Does http://${stageHost}:8080 look good?"
try {
    checkpoint('Before production')
} catch (NoSuchMethodError _) {
    echo 'Checkpoint feature available in Jenkins Enterprise by CloudBees.'
}
stage name: 'Production', concurrency: 1
node('slave') {
    unarchive mapping: ['target/x.war' : 'x.war']
    sh "mvn cargo:redeploy -Dhost=${prodHost}"
    echo 'Deployed to http://${prodHost}:8080'
}
