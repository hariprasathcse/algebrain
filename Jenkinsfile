def env = "$env"
def job=JOB_BASE_NAME-

pipeline{
	agent none
	stages{
		stage('Test Param'){
			steps{
				script{
					println "${env}"
					println "${job}"
				}
			}
		}
	}
}
