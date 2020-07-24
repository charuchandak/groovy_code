# DSL script for Job 1
job("DevOps_Task6_Job1") {
description ("Job to pull code from GitHub repository")
  scm{
    github('https://github.com/charuchandak/task6_devops.git','master')
  }
  triggers {
        scm ("* * * * *")
    }
   steps {
        shell('''
sudo mkdir -p /root/devops_task6
sudo cp -rvf * /root/devops_task6
''')
     
        remoteShell('root@192.168.1.103:22') {
          command('''
if kubectl get pvc | grep html-pvc
then
   echo "HTTPD PVC already created"
else
   kubectl create -f /root/devops_task6/httpd-pvc.yml
   kubectl get pvc
fi
         ''')
     
        remoteShell('root@192.168.1.103:22') {
          command('''
if kubectl get deploy | grep html-dp
then
   echo "HTTPD Pods are running"
else
   kubectl create -f /root/devops_task6/httpd-server.yml
   kubectl get svc
fi
         ''')
    }
     
   }
 }
  
}
# DSL script for Job 2
job("DevOps_Task6_Job2") {
description ("Job to shift code into testing environment")
  triggers {
        upstream('DevOps_Task6_Job1', 'SUCCESS')
    }
   steps {
        remoteShell('root@192.168.1.103:22') {
          command('''
html_pods=$(kubectl get pods -l 'app in (html-dp)' -o jsonpath="{.items[0].metadata.name}")
echo $html_pods
kubectl cp /root/devops_task6/index.html "$html_pods":/usr/local/apache2/htdocs
         ''')
    }
	# DSL Script for Job 3
job("DevOps_Task6_Job3") {
description ("Testing the code")
  triggers {
        upstream('DevOps_Task6_Job2', 'SUCCESS')
    }
   steps {
        remoteShell('root@192.168.1.103:22') {
          command('''
status=$(curl -o /dev/null -sw "%{http_code}" http://192.168.99.100:30909)
if [ $status -eq 200 ]
then
  echo "Page is working well"
  exit 0
else
  echo "Page is not working well"
  exit 1
fi
''')
          
     }
     
   }
  
  publishers {
        extendedEmail {
            recipientList('charulata2796@gmail.com')
            defaultSubject('Build failed')
            defaultContent('Error in build or HTML Page')
            contentType('text/html')
            triggers {
                failure{
          attachBuildLog(true)
                    subject('Failed build')
                    content('The build was failed')
                    sendTo {
                        developers()
                        
                    }
                }
            }
        }
    }
}
#DSL Script for Build Pipeline View(Requires Build Pipeline Plugin)
buildPipelineView('DevOps_Task6') {
    title('DevOps_Task6')
    displayedBuilds(3)
    selectedJob('DevOps_Task6_Job1')
    showPipelineParameters(true)
    refreshFrequency(3)
}
     
   }
}