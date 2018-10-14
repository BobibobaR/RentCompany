package telran.cars;


import java.util.Scanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RentCompanyAppl {


	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(RentCompanyAppl.class , args);
		Scanner scanner =  new Scanner(System.in);
		while(true) {
			System.out.println("Enter exit for gracefull service shutdown");
			String input = scanner.nextLine();
			if ( input.equals("exit") ) break;
		}
		scanner.close();
		context.close();
		
	}

}
