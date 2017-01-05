#!groovy

node
{
    currentBuild.result = "SUCCESS"
	
    try 
	{
       stage "Checkout"
            checkout scm

       stage "Clean"
            sh "./gradlew clean; exit 0"

	   stage "Build"
            sh "./gradlew build"
			
       stage "APKs"
            sh "./gradlew assembleRelease"
            archiveArtifacts artifacts: "Squeezer/build/outputs/apk/*.apk", fingerprint: true
    }
		
    catch (err) 
	{
        currentBuild.result = "FAILURE"
        throw err
    }
}
