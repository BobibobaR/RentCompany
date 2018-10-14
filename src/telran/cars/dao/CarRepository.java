package telran.cars.dao;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import telran.cars.entities.CarJpa;

public interface CarRepository extends 
JpaRepository<CarJpa, String> {

	List<CarJpa> findByRecordsDriverLicenseId(long licenseId);

	

}
