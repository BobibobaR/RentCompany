package telran.cars.service;
import telran.cars.entities.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import telran.cars.dao.CarRepository;
import telran.cars.dao.DriverRepository;
import telran.cars.dao.ModelRepository;
import telran.cars.dao.RecordsRepository;
import telran.cars.dto.Car;
import telran.cars.dto.CarsReturnCode;
import telran.cars.dto.Driver;
import telran.cars.dto.Model;
import telran.cars.dto.RentRecord;
import telran.cars.dto.State;
@Service
@ManagedResource

public class RentCompanyJpa extends AbstractRentCompany {
	
	@ManagedAttribute
	public int getFine_persent() {
		return fine_persent;
	}
	@ManagedAttribute
	public void setFine_persent(int fine_persent) {
		this.fine_persent = fine_persent;
	}
	@ManagedAttribute
	public int getGas_price() {
		return gas_price;
	}
	@ManagedAttribute
	public void setGas_price(int gas_price) {
		this.gas_price = gas_price;
	}

	@Value("${finePersent:20}")
	int fine_persent = super.getFinePercent();
	@Value("${gasPrice:15}")
	int gas_price = super.getGasPrice();
	@Autowired 
	CarRepository cars;
	@Autowired
	DriverRepository drivers;
	@Autowired
	ModelRepository models;
	@Autowired
	RecordsRepository  records;
	
	

	@Override
	@Transactional
	public CarsReturnCode addModel(Model model) {
		if(models.existsById(model.getModelName()))
			return CarsReturnCode.MODEL_EXISTS;
		ModelJpa modelJpa=
	new ModelJpa(model.getModelName(), model.getGasTank(),
			model.getCompany(), model.getCountry(),
			model.getPriceDay());
		models.save(modelJpa);
		return CarsReturnCode.OK;
	}

	@PostConstruct
	public void postConstruct() {
		System.out.println("-------------------------------from PostConstructor:----------------------------------- ");
		System.out.printf("from constructor= %d; gas price = %d \n\n ",fine_persent,gas_price);
	}
	@PreDestroy
	public void preDestroy() {
		System.out.println("-------------------------------from PreDestroy:----------------------------------- ");
		System.out.printf("from constructor= %d; gas price = %d \n\n ",fine_persent,gas_price);
	}
	
	@Override
	@Transactional
	public CarsReturnCode addCar(Car car) {
		
		if(cars.existsById(car.getRegNumber()))
			return CarsReturnCode.CAR_EXISTS;
		ModelJpa modelJpa=models.findById(car.getModelName())
				.orElse(null);
		if(modelJpa==null)
			return CarsReturnCode.NO_MODEL;
		CarJpa carJpa=new CarJpa
	(car.getRegNumber(), car.getColor(),
		car.getState(), car.isInUse(), car.isFlRemoved(), modelJpa);
		cars.save(carJpa);
		return CarsReturnCode.OK;
	}

	@Override
	@Transactional
	public CarsReturnCode addDriver(Driver driver) {
		if(drivers.existsById(driver.getLicenseId()))
			return CarsReturnCode.DRIVER_EXISTS;
		
		long licenseId = driver.getLicenseId();
		String name = driver.getName();
		int birthYear = driver.getBirthYear();
		String phone = driver.getPhone();
		DriverJpa driverJpa = new DriverJpa(licenseId, name, birthYear, phone);
		drivers.save(driverJpa);
		
		return CarsReturnCode.OK;
	}

	@Override
	public Model getModel(String modelName) {
		ModelJpa modelJpa=models.findById(modelName).orElse(null);
		if(modelJpa==null)
			return null;
		return getModelDto(modelJpa);
	}

	private Model getModelDto(ModelJpa modelJpa) {
		return new Model
		(modelJpa.getModelName(),
				modelJpa.getGasTank(), modelJpa.getCompany(),
				modelJpa.getCountry(), modelJpa.getPriceDay());
	}

	@Override
	public Car getCar(String carNumber) {
		CarJpa carJpa=cars.findById(carNumber).orElse(null);
		if(carJpa==null)
			return null;
		Car res = getCarDto(carJpa);
		return res;
	}

	private Car getCarDto(CarJpa carJpa) {
		Car res=new Car(carJpa.getRegNumber(), carJpa.getColor(),
				carJpa.getModel().getModelName());
		res.setFlRemoved(carJpa.isFlRemoved());
		res.setState(carJpa.getState());
		res.setInUse(carJpa.isInUse());
		return res;
	}

	@Override
	public Driver getDriver(long licenseId) {
		DriverJpa driverjpa  = drivers.findById(licenseId).orElse(null);
		
		return driverjpa != null?getDriverDto(driverjpa):null;
	}

	private Driver getDriverDto(DriverJpa driverjpa) {
	
		long licenseId = driverjpa.getLicenseId() ;
		String name = driverjpa.getName();
		int birthYear = driverjpa.getBirthYear();
		String phone = driverjpa.getPhone();
		return new Driver(licenseId, name, birthYear, phone);
	}

	@Override
	@Transactional
	public CarsReturnCode rentCar(String carNumber, long licenseId, LocalDate rentDate, int rentDays) {
		CarJpa carJpa=cars.findById(carNumber).orElse(null);
		if(carJpa==null||carJpa.isFlRemoved())
			return CarsReturnCode.NO_CAR;
		if(carJpa.isInUse())
			return CarsReturnCode.CAR_IN_USE;
		DriverJpa driverJpa=drivers.findById(licenseId).orElse(null);
		if(driverJpa== null)
			return CarsReturnCode.NO_DRIVER;
		RecordJpa recordJpa=new RecordJpa
		(driverJpa, carJpa, rentDate, rentDays);
		carJpa.setInUse(true);
		records.save(recordJpa);
		return CarsReturnCode.OK;
		
	}

	@Override
	@Transactional
	public CarsReturnCode returnCar(String carNumber, long licenseId,
			LocalDate returnDate, int gasTankPercent,int damages) {
		
			RecordJpa recordJpa=records
					.findByCarRegNumberAndReturnDateNull(carNumber);
			if(recordJpa==null)
				return CarsReturnCode.CAR_NOT_RENTED;
			if(returnDate.isBefore(recordJpa.getRentDate()))
				return CarsReturnCode.RETURN_DATE_WRONG;
			recordJpa.setReturnDate(returnDate);
			recordJpa.setDamages(damages);
			CarJpa carJpa=cars.getOne(carNumber);
			recordJpa.setGasTankPercent(gasTankPercent);
			setCost(recordJpa,carJpa.getModel());
			updateCarData(damages,carJpa);
		return CarsReturnCode.OK;
		}
		private void updateCarData(int damages, CarJpa car) {
			if(damages>0 && damages<10)
				car.setState(State.GOOD);
			else if(damages>=10&&damages<30)
				car.setState(State.BAD);
			else if(damages>=30)
				car.setFlRemoved(true);
			car.setInUse(false);
		}

		private void setCost(RecordJpa record, ModelJpa model) {
			long period=ChronoUnit.DAYS.between
					(record.getRentDate(), record.getReturnDate());
			float costPeriod=0;
			
			
			float costGas=0;
			costPeriod = getCostPeriod(record, period, model);
			costGas = getCostGas(record, model);
			record.setCost(costPeriod+costGas);
			
		}
		private float getCostGas(RecordJpa record, ModelJpa model) {
			float costGas;
			int gasTank=model.getGasTank();
			float litersCost=(float)(100-record.getGasTankPercent())*gasTank/100;
			costGas=litersCost*getGasPrice();
			return costGas;
		}
		private float getCostPeriod(RecordJpa record, long period, ModelJpa model) {
			float costPeriod;
			long delta=period-record.getRentDays();
			float additionalPeriodCost=0;
			int pricePerDay=model.getPriceDay();
			int rentDays=record.getRentDays();
			if(delta>0){
				additionalPeriodCost=getAdditionalPeriodCost
						(pricePerDay,delta);
			}
			costPeriod=rentDays*pricePerDay+additionalPeriodCost;
			return costPeriod;
		}

		private float getAdditionalPeriodCost(int pricePerDay, long delta) {
			float fineCostPerDay=pricePerDay*getFinePercent()/100;
			return (pricePerDay+fineCostPerDay)*delta;
		}


	@Override
	@Transactional
	public CarsReturnCode removeCar(String carNumber) {
		CarJpa car=cars.findById(carNumber).orElse(null);
		if(car==null)
			return CarsReturnCode.NO_CAR;
		if(car.isInUse())
			return CarsReturnCode.CAR_IN_USE;
		car.setFlRemoved(true);
		return CarsReturnCode.OK;
	}

	@Override
	public List<Car> clear(LocalDate currentDate, int days) {
		LocalDate dateDelete=currentDate.minusDays(days);
		List<RecordJpa> recordsForDelete=
		records.findByCarFlRemovedTrueAndReturnDateBefore(dateDelete);
		List<CarJpa> carsJpaForDelete=recordsForDelete.stream().map(r->r.getCar())
				.distinct().collect(Collectors.toList());
		carsJpaForDelete.forEach(cars::delete);
		return carsJpaForDelete.stream().map(this::getCarDto)
				.collect(Collectors.toList());
	
	}
	
	@Override
	public List<Driver> getCarDrivers(String carNumber) {
		List<DriverJpa> driverJpa = drivers.findByRecordsCarRegNumber(carNumber);
		return driverJpa.stream().map(this::getDriverDto)
				.collect(Collectors.toList());
		
	}

	@Override
	public List<Car> getDriverCars(long licenseId) {
		List<CarJpa> carJpa = cars.findByRecordsDriverLicenseId(licenseId);
		
		return carJpa.stream().map(this::getCarDto)
				.collect(Collectors.toList());
	}

	@Override
	public Stream<Car> getAllCars() {	
		return cars.findAll().stream().map(this::getCarDto);
	}

	@Override
	public Stream<Driver> getAllDrivers() {
		return drivers.findAll().stream().map(this::getDriverDto);
	}

	@Override
	public Stream<RentRecord> getAllRecords() {
		
		
		return records.findAll().stream().map(this::getRecordDto);
	}

	private RentRecord getRecordDto(RecordJpa recordJpa) {
		
		long licenseId = recordJpa.getDriver().getLicenseId() ;
		String carNumber = recordJpa.getCar().getRegNumber();
		LocalDate rentDate = recordJpa.getRentDate();
		int rentDays = recordJpa.getRentDays();
		LocalDate returnDate = recordJpa.getReturnDate();
		int gasTankPercent = recordJpa.getGasTankPercent();
		float cost =  recordJpa.getCost();
		int damages = recordJpa.getDamages() ;
		RentRecord record = new RentRecord(licenseId, carNumber, rentDate, returnDate, 
				gasTankPercent, rentDays, cost, damages);
		
		return record;
		
	}
	@Override
	public List<String> getAllModelNames() {
		return 	models.findAll().stream()
				                .map(x->x.getModelName())
				                .collect(Collectors.toList());
	}

	@Override
	public List<String> getMostPopularModelNames() {
		//Not implemented
		return null;
	}


	@Override
	public List<String> getMostProfitModelNames() {
		//Not implemented
		return null;
	}
	@Override
	public void save() {
		
	}

	@Override
	public double getModelProfit(String modelName) {
		// Not implemented
		return 0;
	}

}
