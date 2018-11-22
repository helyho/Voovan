package org.voovan.tools;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TSQLUnit {

    public static void main(String[] args) {
        String s = "select * from deposit_request where status != 2 AND `random_deposit_amountx` = ::5 \n" +
                "							 and  mmmk between ::1 and ::4 \n" +
                "							 and  mmm not in (::10, ::11, ::12, ::13)\n" +
                "							 and  mmm in (::10, ::11, ::12, ::13)\n" +
                "                            and  (client_account_name >= ::1 or client_card_number <= ::2) \n" +
                "                            and  (client_account_name != ::3 or client_card_numbe1 > ::4) \n" +
                "                            and  (post_script >= ::6 or deposit_amount = ::7 or random_deposit_amount = ::8)\n" +
                "                            and state = 1";

//		String s = "SELECT count(0) from 90_entrust where 1=1 and status in (0,1,2,3) and user_id = '7hJvVarCrMe' and original_market_id = '90' and type in (0,1) and xx is null";
//		try {
//			DBAccess.newInstance().queryObject(s, String.class);
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}

        System.out.println(TSQL.removeEmptyCondiction(s, TObject.asMap("1", null, "4", null, "7", null, "10", null)));
    }
}
