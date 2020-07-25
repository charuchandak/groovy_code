 job("Task6_Job1") {
description ("Job to pull code from GitHub repository")
  scm{
    github('charuchandak/task6_devops','master')
  }
  triggers {
        scm ("* * * * *")
    }
   steps {
        shell('''
cp -rvf * /root/task6
''')
     
        remoteShell('root@192.168.1.103:22') {
          command('''
if kubectl get pvc | grep myphp-pv-claim
then
   echo "HTTPD PVC already created"
else
   kubectl create -f /root/task6/pvc.yml
fi
         ''')
     
        remoteShell('root@192.168.1.103:22') {
          command('''
if kubectl get deploy | grep php-deploy
then
   echo "HTTPD Pods are running"
else
   kubectl create -f /root/task6/deploy.yml
   kubectl create -f /root/task6/service.yml
   kubectl get svc
fi
         ''')
    }
     
   }
 }
  
}
job("Task6_Job2") {
description ("Job to shift code into testing environment")
  triggers {
        upstream('Task6_Job1', 'SUCCESS')
    }
   steps {
        remoteShell('root@192.168.1.103:22') {
          command('''
html_pods=$(kubectl get pods -l 'app in (php-deploy)' -o jsonpath="{.items[0].metadata.name}")
echo $html_pods
kubectl cp /root/task6/index.html "$html_pods":/var/www/html
         ''')
    }
job("Task6_Job3") {
description ("Testing the code")
  triggers {
        upstream('Task6_Job2', 'SUCCESS')
    }
   steps {
        remoteShell('root@192.168.1.103:22') {
          command('''
status=$(curl -o /dev/null -sw "%{http_code}" http://192.168.99.103:30001)
if [ $status -eq 200 ]
then
  echo "App running"
  exit 0
else
  echo "App not working"
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
buildPipelineView('Task6') {
    title('Task6')
    displayedBuilds(3)
    selectedJob('Task6_Job1')
    showPipelineParameters(true)
    refreshFrequency(3)
}
     
   }
}
