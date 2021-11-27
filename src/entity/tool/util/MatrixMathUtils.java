/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */

package entity.tool.util;

import java.math.BigDecimal;

/**
 * 矩阵基本运算
 */
public class MatrixMathUtils
{
    public static final double OPEROPERATION_ADD = 1;
    public static final double OPEROPERATION_SUB = 2;
    public static final double OPEROPERATION_MUL = 3;

    public static BigDecimal round( double v, int scale ) {
        BigDecimal b = new BigDecimal( Double.toString( v ) );

        BigDecimal one = new BigDecimal( "1" );

        return b.divide( one, scale, BigDecimal.ROUND_HALF_UP );
    }
    
    /**
     * 矩阵加法运算
     * 
     * @param matrix_a
     *            矩阵a
     * @param matrix_b
     *            矩阵b
     * @return result1 运算合法，返回结果
     */
    public static Double[][] additive( Double[][] matrix_a, Double[][] matrix_b )
    {
        Double[][] result1 = new Double[matrix_a.length][matrix_a[0].length];
        if ( legalOperation( matrix_a, matrix_b, OPEROPERATION_ADD ) )
        {
            for ( int i = 0; i < matrix_a.length; i++ )
            {
                for ( int j = 0; j < matrix_a[0].length; j++ )
                {
                    result1[i][j] = matrix_a[i][j] + matrix_b[i][j];
                }
            }
        }
        return result1;
    }

    /**
     * 矩阵减法运算
     * 
     * @param matrix_a
     *            矩阵a
     * @param matrix_b
     *            矩阵b
     * @return result2 运算合法，返回结果
     */
    public static Double[][] subtraction( Double[][] matrix_a, Double[][] matrix_b )
    {
        Double[][] result2 = new Double[matrix_a.length][matrix_a[0].length];
        if ( legalOperation( matrix_a, matrix_b, OPEROPERATION_SUB ) )
        {
            for ( int i = 0; i < matrix_a.length; i++ )
            {
                for ( int j = 0; j < matrix_a[0].length; j++ )
                {
                    result2[i][j] = matrix_a[i][j] - matrix_b[i][j];
                }
            }
        }
        return result2;
    }

    /**
     * 矩阵乘法运算a 矩阵与矩阵相乘
     * 
     * @param doubles
     *            矩阵a
     * @param t0
     *            矩阵b
     * @return result3 运算合法，返回结果; null 运算不合法
     */
    public static Double[][] multiplication( Double[][] doubles, Double[][] t0 )
    {
        Double[][] result3 = new Double[doubles.length][t0[0].length];
        if ( legalOperation( doubles, t0, OPEROPERATION_MUL ) )
        {
            for ( int i = 0; i < doubles.length; i++ )
            {
                for ( int j = 0; j < doubles[0].length; j++ )
                {
                    result3[i][j] = calculateSingleResult( doubles, t0, i, j );
                }
            }
            return result3;
        } else
        {
            return null;
        }
    }

    /**
     * 矩阵乘法运算b 矩阵的数乘
     * 
     * @param matrix_a
     *            矩阵a
     * @param n
     *            数n
     * @return result4 运算合法，返回结果
     */
    public static Double[][] multiplication( Double[][] matrix_a, Double n )
    {
        Double[][] result4 = new Double[matrix_a.length][matrix_a[0].length];
        for ( int i = 0; i < matrix_a.length; i++ )
        {
            for ( int j = 0; j < matrix_a[0].length; j++ )
            {
                result4[i][j] = n * matrix_a[i][j];
            }
        }
        return result4;
    }

    /**
     * 矩阵乘法a中result每个元素的单一运算
     * 
     * @param matrix_a
     *            矩阵a
     * @param matrix_b
     *            矩阵b
     * @param row
     *            参与单一运算的行标
     * @param col
     *            参与单一运算的列标
     * @return result 运算结果
     */
    public static Double calculateSingleResult( Double[][] matrix_a, Double[][] matrix_b, int row, int col )
    {
        Double result = Double.valueOf( 0 );
        for ( int i = 0; i < matrix_a[0].length; i++ )
        {
            result += matrix_a[row][i] * matrix_b[i][col];
        }
        return result;
    }

    /**
     * 判断矩阵是否可以进行合法运算
     * 
     * @param matrix_a
     *            矩阵a
     * @param matrix_b
     *            矩阵b
     * @param type
     *            判断运算类型，是加法，减法，还是乘法运算
     * @return legal true 运算合法; false 运算不合法
     */
    private static boolean legalOperation( Double[][] matrix_a, Double[][] matrix_b, Double type )
    {
        boolean legal = true;
        if ( type == OPEROPERATION_ADD || type == OPEROPERATION_SUB )
        {
            if ( matrix_a.length != matrix_b.length || matrix_a[0].length != matrix_b[0].length )
            {
                legal = false;
            }
        } else if ( type == OPEROPERATION_MUL )
        {
            if ( matrix_a.length != matrix_b[0].length )
            {
                legal = false;
            }
        }
        return legal;
    }
}
