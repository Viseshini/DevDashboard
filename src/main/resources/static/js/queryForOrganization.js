/**
 * @author ViseshiniReddy
 */
angular
		.module('devDashboard', [ 'ngTable' ])
		.controller(
				'queryForOrganization',
				function($scope, $http, NgTableParams) {

					$http
							.get(
									"http://****.****.****.net:8080/requestStructure")
							.success(
									function(response) {
										$scope.startDate = response.startDate;
										$scope.endDate = response.endDate;
										$scope.orgName = response.orgName;
										$scope.teamName = response.teamName;
										$scope.commits = response.commits;
										$scope.prs = response.prs;
										$scope.criticalIssues = response.criticalIssues;
										$scope.codeCoverage = response.codeCoverage;
										$scope.successfulBuilds = response.successfulBuilds;
										$scope.failedBuilds = response.failedBuilds;
										$scope.reviews = response.reviews;
									})
							.error(
									function() {
										alert("Error in loading data from requestStructure")
									});

					$http
							.get(
									"http://****.****.****.net:8080/list")
							.success(function(response) {
								const data = response;
								$scope.tableParams = new NgTableParams({}, {
									dataset : data
								});
							})
							.error(
									function() {
										alert("Error in loading data for the organization table");

									});

					$scope.RankbyScore = function(user) {
						return 1 / user.score;
					}

				});