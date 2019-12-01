(function () {
    'use strict';

    angular.module('myApp', ['ngCookies'])
        .constant('REST_SERVICE_URL', 'http://localhost:8080/jaudio-kafka/rest/')
        .service('restService', ['$http','$q','$log','REST_SERVICE_URL',restService])
        .controller('userController',['$scope','$cookieStore','$window','$log','$sce','restService',userController])
        .controller('editController',['$scope','$cookieStore','$window','$log','restService',editController])
        .controller('registerController',['$scope','$cookieStore','$window','$log','restService',registerController])
        .controller('adminController',['$scope','$cookieStore','$window','$log','$timeout','restService',adminController])
        .controller('topController',['$scope','$cookieStore','$window',
            function ($scope,$cookieStore,$window) {
                var credentials = $cookieStore.get('credentials');
                if (!credentials) $window.location.href = 'index.html';
            }])
        .controller('validateCtrl', ['$scope','$cookieStore','$window','$log','restService',
            function($scope,$cookieStore,$window,$log,restService) {
                $scope.login = function () {
                    $log.log ('User '+$scope.username+' tries to log in...');

                    restService.post('authenticate',{
                        'username': $scope.username,
                        'password': $scope.password
                    })
                        .then(
                        function (response) {
                            if (response.status === 'SUCCESS' &&
                                response.user && response.user.username && response.user.password) {
                                $cookieStore.put('credentials',response.user);
                                $log.log('Successful login for user: ' + $scope.username);
                                $window.location.href = 'user.html';
                            }else{
                                $log.log('Unsuccessful login attempt for user: ' + $scope.username);
                                $scope.username = '';
                                $scope.password = '';
                                $scope.errorMessage= response.message;
                                $scope.errorMessageShow = true;
                            }
                        },
                        function (error) {
                            $log.log('Unable to login user: ' + $scope.username);
                            $log.log(error);
                            $scope.username = '';
                            $scope.password = '';
                        }
                    );
                };
            }]);

    function userController ($scope,$cookieStore,$window,$log,$sce,restService) {
        var userCookie = $cookieStore.get('credentials');
        if (!userCookie) $window.location.href = 'index.html';
        $scope.username = userCookie.username;
        $scope.isAdmin = userCookie.admin;
        $scope.selectedGuid = null;
        $scope.data = userCookie;
        $scope.goEdit = function () {$window.location.href = 'edit.html';};

        $scope.testFFTs = null;
        $scope.trainFFTs = null;
        $scope.timestamps = null;
        $scope.similarities = null;
        $scope.alerts = {};

        function alert (producer,threshold,limit) {
            restService.post('getSimilaritiesByProducer', {
                'username': userCookie.username,
                'password': userCookie.password,
                'producer': producer,
                'threshold': threshold,
                'limit': limit
            })
                .then(
                function (response) {
                    if (response.status === 'SUCCESS') {
                        $log.log ("PRODUCER:"+producer);
                        $log.log ("THRESHOLD:"+threshold);
                        $log.log (response);
                        $scope.alerts[producer] = response.similarities
                    }else{
                        $log.log(response);
                        $scope.errorMessage = response.message;
                        $scope.errorMessageShow = true;
                    }
                },
                function (error) {
                    $log.log(error);
                    $scope.errorMessage = error;
                    $scope.errorMessageShow = true;
                }
            );
        }

        function refresh () {
                    //if ($scope.isAdmin) {
                        restService.post('refreshSimilarities', {
                            'username': userCookie.username,
                            'password': userCookie.password
                        })
                            .then(
                            function (response) {
                                if (response.status === 'SUCCESS') {
                                    //$log.log (response);
                                    $scope.testFFTs = response.testFFTs;
                                    $scope.trainFFTs = response.trainFFTs;
                                    $scope.timestamps = response.timestamps;
                                    $scope.similarities = response.similarities;

                                    var producers = Object.keys($scope.similarities);
                                    for (var i in producers) {
                                        alert (producers[i],80,10);
                                    }
                                }else{
                                    $log.log(response);
                                    $scope.errorMessage = response.message;
                                    $scope.errorMessageShow = true;
                                }
                            },
                            function (error) {
                                $log.log(error);
                                $scope.errorMessage = error;
                                $scope.errorMessageShow = true;
                            }
                        );
                    /*}else{
                        $scope.errorMessage = "* You are not authorized to view this page...";
                        $scope.errorMessageShow = true;
                    }*/
        };
        refresh();

        $scope.$on('refreshEvent', function () {
            $log.log ('Caused refreshEvent');
            refresh();
        });
    }

    function registerController ($scope,$cookieStore,$window,$log,restService) {
        for (var key in $cookieStore) {
            $cookieStore.remove(key);
        }

        $scope.register = function () {
            if ($scope.password1 === $scope.password2) {
                var concatenatedSelectedCountries = "";
                for (var i=0; i<$scope.selectCountries.length; i++) {
                    concatenatedSelectedCountries += ","+$scope.selectCountries[i];
                }
                concatenatedSelectedCountries = concatenatedSelectedCountries.substr(1);
                restService.post('register', {
                    'username': $scope.username,
                    'password': $scope.password1,
                    'firstname': $scope.firstname,
                    'lastname': $scope.lastname,
                    'msisdn': $scope.msisdn,
                    'email': $scope.email,
                    'region': concatenatedSelectedCountries
                })
                    .then(
                    function (response) {
                        if (response.status === 'SUCCESS') {
                            $cookieStore.put('credentials', [$scope.username,$scope.password1,false]);
                            $scope.modalImage = 'images/thumb.jpg';
                            $scope.modalTitle = 'Congratulations';
                            $scope.modalEnable = true;
                        } else {
                            $log.log(response);
                            $scope.errorMessage = response.message;
                            $scope.errorMessageShow = true;
                        }
                    },
                    function (error) {
                        $log.log(error);
                        $scope.errorMessage = error;
                        $scope.errorMessageShow = true;
                    }
                );
            }else{
                $log.log('Passwords do not match!');
                $scope.errorMessage = 'Passwords do not match!';
                $scope.errorMessageShow = true;
            }
        };


        $scope.countries = ['Afghanistan','Albania','Algeria','Andorra','Angola','Antigua and Deps','Argentina','Armenia','Australia','Austria','Azerbaijan','Bahamas','Bahrain','Bangladesh','Barbados','Belarus','Belgium','Belize','Benin','Bhutan','Bolivia','Bosnia Herzegovina','Botswana','Brazil','Brunei','Bulgaria','Burkina','Burundi','Cambodia','Cameroon','Canada','Cape Verde','Central African Rep','Chad','Chile','China','Colombia','Comoros','Congo','Congo Democratic Rep','Costa Rica','Croatia','Cuba','Cyprus','Czech Republic','Denmark','Djibouti','Dominica','Dominican Republic','East Timor','Ecuador','Egypt','El Salvador','Equatorial Guinea','Eritrea','Estonia','Ethiopia','Fiji','Finland','France','Gabon','Gambia','Georgia','Germany','Ghana','Greece','Grenada','Guatemala','Guinea','Guinea Bissau','Guyana','Haiti','Honduras','Hungary','Iceland','India','Indonesia','Iran','Iraq','Ireland Republic','Israel','Italy','Ivory Coast','Jamaica','Japan','Jordan','Kazakhstan','Kenya','Kiribati','Korea North','Korea South','Kosovo','Kuwait','Kyrgyzstan','Laos','Latvia','Lebanon','Lesotho','Liberia','Libya','Liechtenstein','Lithuania','Luxembourg','Macedonia North','Madagascar','Malawi','Malaysia','Maldives','Mali','Malta','Marshall Islands','Mauritania','Mauritius','Mexico','Micronesia','Moldova','Monaco','Mongolia','Montenegro','Morocco','Mozambique','Myanmar (Burma)','Namibia','Nauru','Nepal','Netherlands','New Zealand','Nicaragua','Niger','Nigeria','Norway','Oman','Pakistan','Palau','Panama','Papua New Guinea','Paraguay','Peru','Philippines','Poland','Portugal','Qatar','Romania','Russian Federation','Rwanda','St Kitts & Nevis','St Lucia','Saint Vincent & the Grenadines','Samoa','San Marino','Sao Tome & Principe','Saudi Arabia','Senegal','Serbia','Seychelles','Sierra Leone','Singapore','Slovakia','Slovenia','Solomon Islands','Somalia','South Africa','South Sudan','Spain','Sri Lanka','Sudan','Suriname','Swaziland','Sweden','Switzerland','Syria','Taiwan','Tajikistan','Tanzania','Thailand','Togo','Tonga','Trinidad and Tobago','Tunisia','Turkey','Turkmenistan','Tuvalu','Uganda','Ukraine','United Arab Emirates','United Kingdom','United States','Uruguay','Uzbekistan','Vanuatu','Vatican City','Venezuela','Vietnam','Yemen','Zambia','Zimbabwe'];
        $scope.selectCountries = [];

        $scope.$on('recycleIn', function () {
            var newone = document.getElementById('myOptionBinding').value;
            if ($scope.countries.indexOf(newone) >= 0) {
                if ($scope.selectCountries.indexOf(newone) < 0) {
                    $scope.selectCountries.push(newone);
                }
            }
            console.log ($scope.selectCountries);
        });
        $scope.$on('recycleOut', function () {
            var newone = document.getElementById('myOptionBinding').value;
            var position = $scope.selectCountries.indexOf(newone);
            if (position >= 0) {
                $scope.selectCountries.splice(position,1);
            }
            console.log ($scope.selectCountries);
        });
        $scope.$on('recyclePop', function () {
            if ($scope.selectCountries.length > 0) {
                $scope.selectCountries.splice(-1,1);
            }
            console.log ($scope.selectCountries);
        });
    }

    function adminController ($scope,$cookieStore,$window,$log,$timeout,restService) {
        var userCookie = $cookieStore.get('credentials');
        if (!userCookie) $window.location.href = 'index.html';
        $scope.username = userCookie.username;
        $scope.isAdmin = userCookie.admin;
        var getUsers = function () {
            if ($scope.isAdmin) {
                restService.post('getUsers', {
                    'username': userCookie.username,
                    'password': userCookie.password
                })
                    .then(
                    function (response) {
                        if (response.status === 'SUCCESS') {
                            $scope.users = response.users;
                            console.log($scope.users);
                            $timeout(function () {
                                $("#exampleX").DataTable();
                            }, 1000);
                        } else {
                            $log.log(response);
                            $scope.errorMessage = response.message;
                            $scope.errorMessageShow = true;
                        }
                    },
                    function (error) {
                        $log.log(error);
                        $scope.errorMessage = error;
                        $scope.errorMessageShow = true;
                    }
                );
            }else{
                $scope.users = [];
                $scope.errorMessage = "* You are not authorized to view this page...";
                $scope.errorMessageShow = true;
            }
        };
        $scope.activateUser = function (userToActivate) {
            restService.post ('activateUser',{
                'username': userCookie.username,
                'password': userCookie.password,
                'toActivate': userToActivate.username})
                .then(
                function (response) {
                    if (response.status === 'SUCCESS') {
                        userToActivate.status = 1;
                    }else{
                        $log.log(response);
                        $scope.errorMessage= response.message;
                        $scope.errorMessageShow = true;
                    }
                },
                function (error) {
                    $log.log(error);
                    $scope.errorMessage= error;
                    $scope.errorMessageShow = true;
                }
            );
        };
        $scope.disableUser = function (userToDisable) {
            restService.post ('disableUser',{
                'username': userCookie.username,
                'password': userCookie.password,
                'toDisable': userToDisable.username})
                .then(
                function (response) {
                    if (response.status === 'SUCCESS') {
                        userToDisable.status = 0;
                    }else{
                        $log.log(response);
                        $scope.errorMessage= response.message;
                        $scope.errorMessageShow = true;
                    }
                },
                function (error) {
                    $log.log(error);
                    $scope.errorMessage= error;
                    $scope.errorMessageShow = true;
                }
            );
        };
        getUsers();
    }

    function editController ($scope,$cookieStore,$window,$log,restService) {
        $scope.countries = ['Afghanistan','Albania','Algeria','Andorra','Angola','Antigua and Deps','Argentina','Armenia','Australia','Austria','Azerbaijan','Bahamas','Bahrain','Bangladesh','Barbados','Belarus','Belgium','Belize','Benin','Bhutan','Bolivia','Bosnia Herzegovina','Botswana','Brazil','Brunei','Bulgaria','Burkina','Burundi','Cambodia','Cameroon','Canada','Cape Verde','Central African Rep','Chad','Chile','China','Colombia','Comoros','Congo','Congo Democratic Rep','Costa Rica','Croatia','Cuba','Cyprus','Czech Republic','Denmark','Djibouti','Dominica','Dominican Republic','East Timor','Ecuador','Egypt','El Salvador','Equatorial Guinea','Eritrea','Estonia','Ethiopia','Fiji','Finland','France','Gabon','Gambia','Georgia','Germany','Ghana','Greece','Grenada','Guatemala','Guinea','Guinea Bissau','Guyana','Haiti','Honduras','Hungary','Iceland','India','Indonesia','Iran','Iraq','Ireland Republic','Israel','Italy','Ivory Coast','Jamaica','Japan','Jordan','Kazakhstan','Kenya','Kiribati','Korea North','Korea South','Kosovo','Kuwait','Kyrgyzstan','Laos','Latvia','Lebanon','Lesotho','Liberia','Libya','Liechtenstein','Lithuania','Luxembourg','Macedonia North','Madagascar','Malawi','Malaysia','Maldives','Mali','Malta','Marshall Islands','Mauritania','Mauritius','Mexico','Micronesia','Moldova','Monaco','Mongolia','Montenegro','Morocco','Mozambique','Myanmar (Burma)','Namibia','Nauru','Nepal','Netherlands','New Zealand','Nicaragua','Niger','Nigeria','Norway','Oman','Pakistan','Palau','Panama','Papua New Guinea','Paraguay','Peru','Philippines','Poland','Portugal','Qatar','Romania','Russian Federation','Rwanda','St Kitts & Nevis','St Lucia','Saint Vincent & the Grenadines','Samoa','San Marino','Sao Tome & Principe','Saudi Arabia','Senegal','Serbia','Seychelles','Sierra Leone','Singapore','Slovakia','Slovenia','Solomon Islands','Somalia','South Africa','South Sudan','Spain','Sri Lanka','Sudan','Suriname','Swaziland','Sweden','Switzerland','Syria','Taiwan','Tajikistan','Tanzania','Thailand','Togo','Tonga','Trinidad and Tobago','Tunisia','Turkey','Turkmenistan','Tuvalu','Uganda','Ukraine','United Arab Emirates','United Kingdom','United States','Uruguay','Uzbekistan','Vanuatu','Vatican City','Venezuela','Vietnam','Yemen','Zambia','Zimbabwe'];
        $scope.selectCountries = [];

        var userCookie = $cookieStore.get('credentials');
        if (!userCookie) $window.location.href = 'index.html';
        $scope.username = userCookie.username;
        $scope.isAdmin = userCookie.admin;
        $scope.data = userCookie;
        var whoami = function () {
            if (userCookie) {
                var user = userCookie;
                if (user.region.indexOf(',') >= 0) {
                    var splitRegions = user.region.split(',');
                     for (var i=0; i<splitRegions.length; i+=1) {
                        $scope.selectCountries.push(splitRegions[i]);
                     }
                }else{
                    $scope.selectCountries.push(user.region);
                }
                console.log('$scope.selectCountries: ' + $scope.selectCountries);
            }else{
                $window.location.href = 'index.html';
            }
            document.getElementsByClassName('modal')[0].style.display = 'none';
        };
        whoami();

        $scope.update = function () {
            restService.post ('updateUserDetails',{
                'username': userCookie.username,
                'password': userCookie.password,
                'firstname': $scope.data.firstname,
                'lastname': $scope.data.lastname,
                'email': $scope.data.email,
                'msisdn': $scope.data.msisdn,
                'region': $scope.data.region
            })
                .then(
                function (response) {
                    if (response.status === 'SUCCESS') {
                        $window.location.href = 'user.html';
                    }else{
                        $log.log(response);
                        $scope.errorMessage= response.message;
                        $scope.errorMessageShow = true;
                    }
                    document.getElementsByClassName('modal')[0].style.display = 'none';
                },
                function (error) {
                    $log.log(error);
                    $scope.errorMessage= error;
                    $scope.errorMessageShow = true;
                    document.getElementsByClassName('modal')[0].style.display = 'none';
                }
            );
        };

        $scope.$on('recycleIn', function () {
            var newone = document.getElementById('myOptionBinding').value;
            if ($scope.countries.indexOf(newone) >= 0) {
                if ($scope.selectCountries.indexOf(newone) < 0) {
                    $scope.selectCountries.push(newone);
                }
            }
        });
        $scope.$on('recycleOut', function () {
            var newone = document.getElementById('myOptionBinding').value;
            var position = $scope.selectCountries.indexOf(newone);
            if (position >= 0) {
                $scope.selectCountries.splice(position,1);
            }
        });
    }

    function restService ($http,$q,$log,REST_SERVICE_URL) {
        return {
            get : function (suffix) {
                var deferred = $q.defer();
                $http({
                    method: 'GET',
                    url: REST_SERVICE_URL + suffix,
                    parameters: {}
                }).then(
                    function (response) {
                        $log.log(response.data);
                        deferred.resolve(response.data);
                    }, function (error) {
                        $log.log("ERROR COMMUNICATION WITH " + REST_SERVICE_URL + suffix);
                        $log.log(error);
                        deferred.reject(error);
                    });
                return deferred.promise;
            },
            post : function (suffix,body) {
                var deferred = $q.defer();
                $http({
                    method: 'POST',
                    url: REST_SERVICE_URL + suffix,
                    headers: {'Content-Type': 'application/json'},
                    data: body
                }).then(
                    function (response) {
                        $log.log(response.data);
                        deferred.resolve(response.data);
                    }, function (error) {
                        $log.log("ERROR COMMUNICATION WITH " + REST_SERVICE_URL + suffix);
                        $log.log(error);
                        deferred.reject(error);
                    });
                return deferred.promise;
            }
        };
    }
})();
