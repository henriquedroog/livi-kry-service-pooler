const listContainer = document.querySelector('#service-list');
let servicesRequest = new Request('/service');
fetch(servicesRequest)
.then(function(response) { return response.json(); })
.then(function(serviceList) {
  serviceList.forEach(service => {
    let li = document.createElement("li");
    li.appendChild(document.createTextNode(service.url + ': ' + service.name + ': ' + service.created_at + ': ' + service.status));
    li.appendChild(document.createElement("span"));
    li.appendChild(buildDeleteButton(service.url));
    li.setAttribute('class', 'list-group-item');
    listContainer.appendChild(li);
  });
});

function buildDeleteButton(urlName) {
  const deleteBtn = document.createElement("button");
  deleteBtn.innerHTML = "Delete";
  deleteBtn.onclick = function() {deleteServiceFunction(urlName)};
  deleteBtn.setAttribute('class', 'btn btn-danger');
  return deleteBtn;
}

function deleteServiceFunction(urlName) {
  fetch('/service', {
    method: 'delete',
    headers: {
      'Accept': 'application/json, text/plain, */*',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({url:urlName})
  }).then(res=>location.reload());
}

function parseDate(stringDate) {
  const createdAt = new Date(stringDate);
}

const saveButton = document.querySelector('#post-service');
saveButton.onclick = evt => {
    const urlName = document.querySelector('#url-name').value;
    const svcName = document.querySelector('#svc-name').value;
  
    fetch('/service', {
    method: 'post',
    headers: {
    'Accept': 'application/json, text/plain, */*',
    'Content-Type': 'application/json'
    },
    body: JSON.stringify({url:urlName, name:svcName})
  }).then(res=> location.reload());
}