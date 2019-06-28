def job=JOB_BASE_NAME

pipeline{
	agent none
	stages{
		stage('Test Param'){
			steps{
				script{
					callfunc {
						env_var = "${params.env}"
						job_name = "$job"
					}	
					
					
				}
			}
		}
	}
}
