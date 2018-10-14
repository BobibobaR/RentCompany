package telran.cars.dao;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import telran.cars.dto.Driver;
import telran.cars.entities.DriverJpa;

public interface DriverRepository extends
JpaRepository<DriverJpa, Long> {
    
//	@Query("select p from DriverJpa  p join p.records a where a.driver.")
//	List<Driver>  findCarDrivers(String carNumber);

	List<DriverJpa> findByRecordsCarRegNumber(String carNumber);

	

}
